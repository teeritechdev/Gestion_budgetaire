package com.budget.backend.security.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Adresse IP réelle du poste à l'origine d'une requête, pour le journal d'audit.
 * <ul>
 *   <li>X-Forwarded-For / X-Real-IP ne sont lus que si la requête arrive d'un proxy de confiance
 *       (sinon un client pourrait inscrire n'importe quelle adresse) ;</li>
 *   <li>une requête émise depuis la machine du serveur (127.0.0.1, ::1) est enregistrée avec
 *       l'adresse réseau de cette machine plutôt qu'avec l'adresse de bouclage ;</li>
 *   <li>les formats sont normalisés (::ffff:a.b.c.d → a.b.c.d).</li>
 * </ul>
 */
@Component
public class AdresseIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})(\\.\\d{1,3}){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:.]{2,45}$");
    private static final List<String> INTERFACES_VIRTUELLES =
            List.of("virtual", "vethernet", "vmware", "virtualbox", "hyper-v", "docker", "wsl", "loopback", "vpn", "tap");
    private static final long DUREE_CACHE_MS = 5 * 60 * 1000;

    private final Set<String> proxiesDeConfiance;
    private volatile String adresseMachine;
    private volatile long adresseMachineCalculeeLe;

    public AdresseIpResolver(@Value("${audit.proxies-de-confiance:}") List<String> proxies) {
        this.proxiesDeConfiance = proxies.stream().map(AdresseIpResolver::normaliser)
                .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    public String adresseClient(HttpServletRequest request) {
        String adresse = normaliser(request.getRemoteAddr());
        if (adresse != null && proxiesDeConfiance.contains(adresse)) {
            String transmise = adresseTransmise(request);
            if (transmise != null) adresse = transmise;
        }
        if (adresse == null) return null;
        return estBouclage(adresse) ? Objects.requireNonNullElse(adresseMachine(), adresse) : adresse;
    }

    /** Première adresse valide de X-Forwarded-For (le client d'origine), sinon X-Real-IP. */
    private static String adresseTransmise(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null) {
            for (String partie : xff.split(",")) {
                String adresse = normaliser(partie);
                if (adresse != null) return adresse;
            }
        }
        return normaliser(request.getHeader("X-Real-IP"));
    }

    /** Adresse normalisée, ou null si la valeur n'est pas une adresse IP. */
    static String normaliser(String valeur) {
        if (valeur == null) return null;
        String adresse = valeur.trim();
        if (adresse.startsWith("[") && adresse.endsWith("]")) adresse = adresse.substring(1, adresse.length() - 1);
        int zone = adresse.indexOf('%');
        if (zone > 0) adresse = adresse.substring(0, zone);
        String minuscule = adresse.toLowerCase(Locale.ROOT);
        if (minuscule.startsWith("::ffff:") && IPV4.matcher(adresse.substring(7)).matches()) {
            adresse = adresse.substring(7);
        }
        if (IPV4.matcher(adresse).matches()) {
            for (String octet : adresse.split("\\.")) {
                if (Integer.parseInt(octet) > 255) return null;
            }
            return adresse;
        }
        if (adresse.contains(":") && IPV6.matcher(adresse).matches()) {
            return "0:0:0:0:0:0:0:1".equals(minuscule) ? "::1" : minuscule;
        }
        return null;
    }

    static boolean estBouclage(String adresse) {
        return "::1".equals(adresse) || adresse.startsWith("127.");
    }

    /** Adresse réseau de la machine du serveur (recalculée toutes les 5 minutes : DHCP, Wi-Fi…). */
    String adresseMachine() {
        long maintenant = System.currentTimeMillis();
        if (adresseMachine == null || maintenant - adresseMachineCalculeeLe > DUREE_CACHE_MS) {
            adresseMachine = calculerAdresseMachine();
            adresseMachineCalculeeLe = maintenant;
        }
        return adresseMachine;
    }

    private static String calculerAdresseMachine() {
        // Interface de la route par défaut : un « connect » UDP n'envoie aucun paquet.
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 53);
            InetAddress locale = socket.getLocalAddress();
            if (locale instanceof Inet4Address && !locale.isAnyLocalAddress() && !locale.isLoopbackAddress()) {
                return locale.getHostAddress();
            }
        } catch (Exception ignored) {
            // pas de route par défaut (poste hors réseau) : on parcourt les interfaces
        }
        try {
            for (NetworkInterface carte : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                String nom = (carte.getName() + " " + carte.getDisplayName()).toLowerCase(Locale.ROOT);
                if (!carte.isUp() || carte.isLoopback() || INTERFACES_VIRTUELLES.stream().anyMatch(nom::contains)) continue;
                for (InetAddress adresse : Collections.list(carte.getInetAddresses())) {
                    if (adresse instanceof Inet4Address && !adresse.isLoopbackAddress() && !adresse.isLinkLocalAddress()) {
                        return adresse.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
            // aucune interface exploitable : l'adresse de bouclage sera conservée
        }
        return null;
    }
}
