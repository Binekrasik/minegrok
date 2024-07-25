package dev.binekrasik.minegrok;

import com.github.alexdlaird.ngrok.conf.JavaNgrokConfig;
import com.github.alexdlaird.ngrok.installer.NgrokInstaller;
import com.github.alexdlaird.ngrok.installer.NgrokVersion;
import com.github.alexdlaird.ngrok.NgrokClient;
import com.github.alexdlaird.ngrok.protocol.CreateTunnel;
import com.github.alexdlaird.ngrok.protocol.Proto;
import com.github.alexdlaird.ngrok.protocol.Region;
import com.github.alexdlaird.ngrok.protocol.Tunnel;

import java.util.Objects;

import org.bukkit.plugin.java.JavaPlugin;

public final class Minegrok extends JavaPlugin {
    private NgrokClient ngrokClient;
    private String publicIp;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();

        int ngrokPort = this.getServer().getPort();
        String ngrokRegion = Objects.requireNonNull( this.getConfig().getString( "NGROK_SETTINGS.REGION" ) ).toUpperCase();

        final JavaNgrokConfig javaNgrokConfig = new JavaNgrokConfig.Builder()
                .withNgrokVersion( NgrokVersion.V3 )
                .withRegion( Region.valueOf( ngrokRegion ) )
                .build();

        this.ngrokClient = new NgrokClient.Builder()
                .withNgrokInstaller( new NgrokInstaller() )
                .withJavaNgrokConfig( javaNgrokConfig )
                .build();

        this.ngrokClient.getNgrokProcess().setAuthToken( this.getConfig().getString( "NGROK_SETTINGS.AUTH_TOKEN" ) );

        final CreateTunnel createTunnel = new CreateTunnel.Builder()
                .withProto( Proto.TCP )
                .withAddr( ngrokPort ) // Use the configured Ngrok port
                .build();

        final Tunnel tunnel = ngrokClient.connect( createTunnel );
        this.publicIp = tunnel.getPublicUrl().replace( "tcp://", "" );

        // Log the IP after the server fully starts, so it'll be easier to find it.
        getServer().getScheduler().scheduleSyncDelayedTask( this, () -> getLogger().info( "Ngrok started on port " + ngrokPort + ", IP: " + publicIp ) );
    }

    @Override
    public void onDisable() {
        if ( ngrokClient != null && publicIp != null ) {
            this.ngrokClient.disconnect( publicIp );
            this.ngrokClient.kill();
        }

        this.saveConfig();
    }
}
