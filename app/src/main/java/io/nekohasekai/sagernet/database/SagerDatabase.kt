package io.nekohasekai.sagernet.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.matrix.roomigrant.GenerateRoomMigrations
import io.nekohasekai.sagernet.Key
import io.nekohasekai.sagernet.SagerNet
import io.nekohasekai.sagernet.fmt.KryoConverters
import io.nekohasekai.sagernet.fmt.gson.GsonConverters
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Database(
    entities = [ProxyGroup::class, ProxyEntity::class, RuleEntity::class],
    version = 13,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9, spec = SagerDatabase.Migration8to9::class),
        AutoMigration(from = 9, to = 10, spec = SagerDatabase.Migration9to10::class),
        // 10 -> 11 only adds the nullable awgBean and trustTunnelBean columns.
        AutoMigration(from = 10, to = 11),
        // 11 -> 12 only adds the nullable tailscaleBean column.
        AutoMigration(from = 11, to = 12),
        // 12 -> 13 only adds the rules.gateway column (defaults to 0).
        AutoMigration(from = 12, to = 13)
    ]
)
@TypeConverters(value = [KryoConverters::class, GsonConverters::class])
@GenerateRoomMigrations
abstract class SagerDatabase : RoomDatabase() {

    // Drops the ShadowsocksR (ssrBean) column removed together with SSR support.
    @DeleteColumn(tableName = "proxy_entities", columnName = "ssrBean")
    class Migration8to9 : AutoMigrationSpec

    // Drops the columns of the profile types that went away with external plugin
    // support: Trojan-Go (plugin-only, upstream dead) and the Neko plugin system.
    @DeleteColumn.Entries(
        DeleteColumn(tableName = "proxy_entities", columnName = "trojanGoBean"),
        DeleteColumn(tableName = "proxy_entities", columnName = "nekoBean")
    )
    class Migration9to10 : AutoMigrationSpec {
        // Dropping a bean column leaves its rows behind, and a row whose type no
        // longer has a bean kills requireBean() as soon as the profile list binds
        // it. Type ids are spelled out because they must keep meaning what they
        // meant at this schema version: 3 = ShadowsocksR, whose rows the 8 -> 9
        // migration forgot, 7 = Trojan-Go, 999 = Neko.
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM proxy_entities WHERE type IN (3, 7, 999)")
        }
    }

    companion object {
        @OptIn(DelicateCoroutinesApi::class)
        @Suppress("EXPERIMENTAL_API_USAGE")
        val instance by lazy {
            SagerNet.application.getDatabasePath(Key.DB_PROFILE).parentFile?.mkdirs()
            Room.databaseBuilder(SagerNet.application, SagerDatabase::class.java, Key.DB_PROFILE)
//                .addMigrations(*SagerDatabase_Migrations.build())
                .setJournalMode(JournalMode.TRUNCATE)
                .allowMainThreadQueries()
                .enableMultiInstanceInvalidation()
                .fallbackToDestructiveMigration()
                .setQueryExecutor { GlobalScope.launch { it.run() } }
                .build()
        }

        val groupDao get() = instance.groupDao()
        val proxyDao get() = instance.proxyDao()
        val rulesDao get() = instance.rulesDao()

    }

    abstract fun groupDao(): ProxyGroup.Dao
    abstract fun proxyDao(): ProxyEntity.Dao
    abstract fun rulesDao(): RuleEntity.Dao

}
