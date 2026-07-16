package io.nekohasekai.sagernet.fmt.tailscale;

import androidx.annotation.NonNull;

import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;

import org.jetbrains.annotations.NotNull;

import io.nekohasekai.sagernet.fmt.AbstractBean;
import io.nekohasekai.sagernet.fmt.KryoConverters;
import moe.matsuri.nb4a.utils.JavaUtil;

/**
 * Tailscale joins a mesh instead of dialing a server, so the inherited
 * serverAddress/serverPort mean nothing here and stay at their defaults. What
 * takes their place is the coordination server: controlURL, empty for
 * Tailscale's own.
 */
public class TailscaleBean extends AbstractBean {

    public static final String DEFAULT_CONTROL_URL = "controlplane.tailscale.com";

    public String authKey;
    public String controlURL;
    public String hostname;
    public Boolean ephemeral;
    public Boolean acceptRoutes;
    public String exitNode;
    public Boolean exitNodeAllowLANAccess;

    @Override
    public void initializeDefaultValues() {
        super.initializeDefaultValues();
        if (authKey == null) authKey = "";
        if (controlURL == null) controlURL = "";
        if (hostname == null) hostname = "";
        if (ephemeral == null) ephemeral = false;
        if (acceptRoutes == null) acceptRoutes = true;
        if (exitNode == null) exitNode = "";
        if (exitNodeAllowLANAccess == null) exitNodeAllowLANAccess = false;
    }

    @Override
    public String displayAddress() {
        if (JavaUtil.isNotBlank(controlURL)) {
            return controlURL;
        }
        return DEFAULT_CONTROL_URL;
    }

    @Override
    public String displayName() {
        if (JavaUtil.isNotBlank(name)) {
            return name;
        }
        return "Tailscale";
    }

    // Both probes go to serverAddress, which a Tailscale profile does not have --
    // they would measure the placeholder rather than the tunnel.
    @Override
    public boolean canTCPing() {
        return false;
    }

    @Override
    public boolean canICMPing() {
        return false;
    }

    @Override
    public void serialize(ByteBufferOutput output) {
        output.writeInt(0);
        super.serialize(output);
        output.writeString(authKey);
        output.writeString(controlURL);
        output.writeString(hostname);
        output.writeBoolean(ephemeral);
        output.writeBoolean(acceptRoutes);
        output.writeString(exitNode);
        output.writeBoolean(exitNodeAllowLANAccess);
    }

    @Override
    public void deserialize(ByteBufferInput input) {
        int version = input.readInt();
        super.deserialize(input);
        authKey = input.readString();
        controlURL = input.readString();
        hostname = input.readString();
        ephemeral = input.readBoolean();
        acceptRoutes = input.readBoolean();
        exitNode = input.readString();
        exitNodeAllowLANAccess = input.readBoolean();
    }

    @NotNull
    @Override
    public TailscaleBean clone() {
        return KryoConverters.deserialize(new TailscaleBean(), KryoConverters.serialize(this));
    }

    public static final Creator<TailscaleBean> CREATOR = new CREATOR<TailscaleBean>() {
        @NonNull
        @Override
        public TailscaleBean newInstance() {
            return new TailscaleBean();
        }

        @Override
        public TailscaleBean[] newArray(int size) {
            return new TailscaleBean[size];
        }
    };
}
