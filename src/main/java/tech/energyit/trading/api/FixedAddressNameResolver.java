package tech.energyit.trading.api;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FixedAddressNameResolver extends NameResolver {

    private static Logger log = LoggerFactory.getLogger(FixedAddressNameResolver.class);
    private List<String> hostPorts;

    private Listener listener;

    public FixedAddressNameResolver(List<String> hostPorts) {
        this.hostPorts = hostPorts;
    }

    @Override
    public String getServiceAuthority() {
        return hostPorts.get(0);
    }

    @Override
    public void start(Listener listener) {
        this.listener = listener;

        loadServiceNodes();
    }

    private void loadServiceNodes() {
        List<EquivalentAddressGroup> addrs = new ArrayList<>();

        for (String hostPort : this.hostPorts) {
            String[] tokens = hostPort.split(":");

            String host = tokens[0];
            int port = Integer.parseInt(tokens[1]);
            log.info("static host: [{}], port: [{}]", host, port);

            List<SocketAddress> sockaddrsList = new ArrayList<SocketAddress>();
            sockaddrsList.add(new InetSocketAddress(host, port));
            addrs.add(new EquivalentAddressGroup(sockaddrsList));
        }

        if (!addrs.isEmpty()) {
            this.listener.onAddresses(addrs, Attributes.EMPTY);
        }
    }

    @Override
    public void shutdown() {

    }

    public static class FixedAddressNameResolverProvider extends NameResolverProvider {

        private List<String> hostPorts;

        public FixedAddressNameResolverProvider(List<String> hostPorts) {
            this.hostPorts = hostPorts;
        }

        @Override
        protected boolean isAvailable() {
            return true;
        }

        @Override
        protected int priority() {
            return 5;
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI targetUri, Helper helper) {
            return  new FixedAddressNameResolver(this.hostPorts);
        }

        @Nullable
        @Override
        public NameResolver newNameResolver(URI uri, Attributes attributes) {
            return new FixedAddressNameResolver(this.hostPorts);
        }

        @Override
        public String getDefaultScheme() {
            return "consul";
        }
    }
}