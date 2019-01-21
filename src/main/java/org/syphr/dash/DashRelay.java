/*
 * Copyright 2017 Gregory Moyer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syphr.dash;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.pcap4j.util.MacAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.syphr.dash.pcap.PacketCapturingHandler;
import org.syphr.dash.pcap.PacketCapturingService;
import org.syphr.dash.pcap.PcapNetworkInterfaceListener;
import org.syphr.dash.pcap.PcapNetworkInterfaceService;
import org.syphr.dash.pcap.PcapNetworkInterfaceWrapper;
import org.syphr.dash.pcap.PcapUtil;

public class DashRelay implements PcapNetworkInterfaceListener
{
    private static final Logger LOG = LoggerFactory.getLogger(DashRelay.class);

    private final Properties config;

    private PacketCapturingService packetCapturingService;
    private long lastCommandHandled = 0;

    public DashRelay() throws FileNotFoundException, IOException
    {
        this(Paths.get("config.properties"));
    }

    public DashRelay(Path configPath) throws FileNotFoundException, IOException
    {
        config = new Properties();

        try (InputStream in = new FileInputStream(configPath.toFile()))
        {
            config.load(in);
        }
    }

    public void start()
    {
        PcapNetworkInterfaceService.instance().registerListener(this);
        final String pcapNetworkInterfaceName = config.getProperty("pcapNetworkInterfaceName");
        final String macAddress = config.getProperty("macAddress");
        final Integer packetInterval = Integer.parseInt(config.getProperty("packetInterval"));

        PcapNetworkInterfaceWrapper pcapNetworkInterface = PcapUtil.getNetworkInterfaceByName(pcapNetworkInterfaceName);
        if (pcapNetworkInterface == null)
        {
            LOG.error("The networkinterface '{}' is not present.", pcapNetworkInterfaceName);
            return;
        }

        packetCapturingService = new PacketCapturingService(pcapNetworkInterface);
        boolean capturingStarted = packetCapturingService.startCapturing(new PacketCapturingHandler()
        {

            @Override
            public void packetCaptured(MacAddress macAddress)
            {
                long now = System.currentTimeMillis();
                if (lastCommandHandled + packetInterval < now)
                {
                    trigger(macAddress);
                    lastCommandHandled = now;
                }
            }
        }, macAddress);

        if (capturingStarted)
        {
            LOG.info("Packet capturing on '{}' has started.", pcapNetworkInterface);
        }
        else
        {
            LOG.error("The capturing for '{}' cannot be started.", pcapNetworkInterfaceName);
        }
    }

    @Override
    public void onPcapNetworkInterfaceAdded(PcapNetworkInterfaceWrapper newNetworkInterface)
    {
        if (packetCapturingService != null)
        {
            final PcapNetworkInterfaceWrapper trackedPcapNetworkInterface = packetCapturingService.getPcapNetworkInterface();
            if (trackedPcapNetworkInterface.equals(newNetworkInterface))
            {
                LOG.info("The network interface '{}' is online.", newNetworkInterface.getName());
            }
        }
    }

    @Override
    public void onPcapNetworkInterfaceRemoved(PcapNetworkInterfaceWrapper removedNetworkInterface)
    {
        if (packetCapturingService != null)
        {
            final PcapNetworkInterfaceWrapper trackedPcapNetworkInterface = packetCapturingService.getPcapNetworkInterface();
            if (trackedPcapNetworkInterface.equals(removedNetworkInterface))
            {
                LOG.warn("The network interface '{}' is not present anymore.",
                         removedNetworkInterface.getName());
            }
        }
    }

    protected void trigger(MacAddress macAddress)
    {
        LOG.info("Button press detected from '{}'", macAddress);

        String url = config.getProperty("url");
        String item = config.getProperty("item");
        String command = config.getProperty("command", "TOGGLE");

        // TODO redesign
        try
        {
            HttpResponse response = Request.Post(url + "/rest/items/" + item)
                                           .bodyByteArray(command.getBytes())
                                           .execute()
                                           .returnResponse();

            int status = response.getStatusLine().getStatusCode();
            if (status != 200)
            {
                LOG.error("Trigger request failed with HTTP status {}", status);
            }
        }
        catch (IOException e)
        {
            LOG.error("Trigger request failed", e);
        }
    }

    public static void main(String[] args)
    {
        try
        {
            DashRelay relay = args.length > 0 ? new DashRelay(Paths.get(args[0])) : new DashRelay();
            relay.start();
        }
        catch (FileNotFoundException e)
        {
            System.out.println(e.getMessage());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
