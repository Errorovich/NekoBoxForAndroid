package io.nekohasekai.sagernet.fmt.trusttunnel;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;

public class TrustTunnelBean extends AbstractBean {

    public String username;
    public String password;
    public String sni;
    public String certificates;
    public Boolean allowInsecure;
    /** The link's upstream_protocol: false is HTTP/2, true is HTTP/3 over QUIC. */
    public Boolean quic;
    public Boolean healthCheck;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (username == null) username = "";
        if (password == null) password = "";
        if (sni == null) sni = "";
        if (certificates == null) certificates = "";
        if (allowInsecure == null) allowInsecure = false;
        if (quic == null) quic = false;
        if (healthCheck == null) healthCheck = false;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(username);
        output.writeString(password);
        output.writeString(sni);
        output.writeString(certificates);
        output.writeBoolean(allowInsecure);
        output.writeBoolean(quic);
        output.writeBoolean(healthCheck);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        username = input.readString();
        password = input.readString();
        sni = input.readString();
        certificates = input.readString();
        allowInsecure = input.readBoolean();
        quic = input.readBoolean();
        healthCheck = input.readBoolean();
    }

    @NotNull
    @Override
    public TrustTunnelBean clone() {
        return KryoConverters.deserialize(new TrustTunnelBean(), KryoConverters.serialize(this));
    }

    public static final Creator<TrustTunnelBean> CREATOR = new CREATOR<TrustTunnelBean>() {
        @NonNull
        @Override
        public TrustTunnelBean newInstance() {
            return new TrustTunnelBean();
        }

        @Override
        public TrustTunnelBean[] newArray(int size) {
            return new TrustTunnelBean[size];
        }
    };
}
