package org.commonjava.aprox.bind.vertx.boot;

import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.kohsuke.args4j.Option;

public class BootOptions
{

    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-i", aliases = { "--interface", "--bind", "--listen" }, usage = "Bind to a particular IP address (default: 0.0.0.0, or all available)" )
    private String bind = "0.0.0.0";

    @Option( name = "-p", aliases = { "--port" }, usage = "Use different port (default: 8080)" )
    private int port = 8080;

    @Option( name = "-c", aliases = { "--config" }, usage = "Use an alternative configuration file (default: /etc/aprox/main.conf)" )
    private String config;

    private Weld weld;

    private WeldContainer container;

    public boolean isHelp()
    {
        return help;
    }

    public String getBind()
    {
        return bind;
    }

    public int getPort()
    {
        return port;
    }

    public String getConfig()
    {
        return config;
    }

    public BootOptions setHelp( final boolean help )
    {
        this.help = help;
        return this;
    }

    public BootOptions setBind( final String bind )
    {
        this.bind = bind;
        return this;
    }

    public BootOptions setPort( final int port )
    {
        this.port = port;
        return this;
    }

    public BootOptions setConfig( final String config )
    {
        this.config = config;
        return this;
    }

    public void setWeldComponents( final Weld weld, final WeldContainer container )
    {
        this.weld = weld;
        this.container = container;
    }

    public Weld getWeld()
    {
        return weld;
    }

    public WeldContainer getWeldContainer()
    {
        return container;
    }

}
