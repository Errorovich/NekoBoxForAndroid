package io.nekohasekai.sagernet.fmt.awg;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

/**
 * AmneziaWG. Kept apart from {@link io.nekohasekai.sagernet.fmt.wireguard.WireGuardBean}
 * because the obfuscation parameters below are not a client-side preference: they
 * describe how the server mangles its packets, so a profile only works when they
 * match it exactly.
 */
public class AWGBean extends AbstractBean {

    public String localAddress;
    public String privateKey;
    public String peerPublicKey;
    public String peerPreSharedKey;
    public Integer mtu;
    public Integer persistentKeepalive;

    // Junk packets sent before the handshake. The core rejects a non-positive
    // value for any of the three, and picks each junk size from the jmin..jmax
    // range -- an inverted range makes it panic, so keep jmin <= jmax.
    public Integer jc;
    public Integer jmin;
    public Integer jmax;

    // Junk prepended to the handshake init (s1), response (s2), cookie (s3) and
    // transport (s4) packets.
    public Integer s1;
    public Integer s2;
    public Integer s3;
    public Integer s4;

    // Magic headers replacing WireGuard's message types 1-4. Each is either a
    // number or a "start-end" range, and the four must not overlap. Empty means
    // the standard WireGuard value, which is what makes a bare AWG profile behave
    // like plain WireGuard.
    public String h1;
    public String h2;
    public String h3;
    public String h4;

    // AmneziaWG 1.5 packet templates.
    public String i1;
    public String i2;
    public String i3;
    public String i4;
    public String i5;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (localAddress == null) localAddress = "";
        if (privateKey == null) privateKey = "";
        if (peerPublicKey == null) peerPublicKey = "";
        if (peerPreSharedKey == null) peerPreSharedKey = "";
        if (mtu == null) mtu = 1420;
        if (persistentKeepalive == null) persistentKeepalive = 0;
        if (jc == null) jc = 0;
        if (jmin == null) jmin = 0;
        if (jmax == null) jmax = 0;
        if (s1 == null) s1 = 0;
        if (s2 == null) s2 = 0;
        if (s3 == null) s3 = 0;
        if (s4 == null) s4 = 0;
        if (h1 == null) h1 = "";
        if (h2 == null) h2 = "";
        if (h3 == null) h3 = "";
        if (h4 == null) h4 = "";
        if (i1 == null) i1 = "";
        if (i2 == null) i2 = "";
        if (i3 == null) i3 = "";
        if (i4 == null) i4 = "";
        if (i5 == null) i5 = "";
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(localAddress);
        output.writeString(privateKey);
        output.writeString(peerPublicKey);
        output.writeString(peerPreSharedKey);
        output.writeInt(mtu);
        output.writeInt(persistentKeepalive);
        output.writeInt(jc);
        output.writeInt(jmin);
        output.writeInt(jmax);
        output.writeInt(s1);
        output.writeInt(s2);
        output.writeInt(s3);
        output.writeInt(s4);
        output.writeString(h1);
        output.writeString(h2);
        output.writeString(h3);
        output.writeString(h4);
        output.writeString(i1);
        output.writeString(i2);
        output.writeString(i3);
        output.writeString(i4);
        output.writeString(i5);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        localAddress = input.readString();
        privateKey = input.readString();
        peerPublicKey = input.readString();
        peerPreSharedKey = input.readString();
        mtu = input.readInt();
        persistentKeepalive = input.readInt();
        jc = input.readInt();
        jmin = input.readInt();
        jmax = input.readInt();
        s1 = input.readInt();
        s2 = input.readInt();
        s3 = input.readInt();
        s4 = input.readInt();
        h1 = input.readString();
        h2 = input.readString();
        h3 = input.readString();
        h4 = input.readString();
        i1 = input.readString();
        i2 = input.readString();
        i3 = input.readString();
        i4 = input.readString();
        i5 = input.readString();
    }

    @Override
    public boolean canTCPing() {
        return false;
    }

    @NotNull
    @Override
    public AWGBean clone() {
        return KryoConverters.deserialize(new AWGBean(), KryoConverters.serialize(this));
    }

    public static final Creator<AWGBean> CREATOR = new CREATOR<AWGBean>() {
        @NonNull
        @Override
        public AWGBean newInstance() {
            return new AWGBean();
        }

        @Override
        public AWGBean[] newArray(int size) {
            return new AWGBean[size];
        }
    };
}
