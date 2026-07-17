package io.nekohasekai.sagernet.database;

import android.os.Parcel;

/**
 * see: https://youtrack.jetbrains.com/issue/KT-19853
 */
public class ParcelizeBridge {

    public static RuleEntity createRule(Parcel parcel) {
        return (RuleEntity) RuleEntity.CREATOR.createFromParcel(parcel);
    }

    public static RuleEntityV1 createRuleV1(Parcel parcel) {
        return (RuleEntityV1) RuleEntityV1.CREATOR.createFromParcel(parcel);
    }
}
