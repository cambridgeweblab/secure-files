package ucles.weblab.common.files.domain.s3;


import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;


/**
 *
 * @author Sukhraj
 */
public final class WeblabClientConfiguration extends ClientConfiguration {
    
    public WeblabClientConfiguration(boolean secure, int maxRetries){
        
        this(secure);
        this.setMaxErrorRetry(maxRetries);
    }
    
    /**
     * Not sure if this is needed yet. 
     * @param secure 
     */
    public WeblabClientConfiguration(boolean secure) {
        if (secure) {
            this.setProtocol(Protocol.HTTPS);
        }
        else {
            this.setProtocol(Protocol.HTTP);
        }
        String proxyHost = System.getProperty("http.proxyHost");
        if (proxyHost != null && !proxyHost.trim().isEmpty()) {
            this.setProxyHost(proxyHost);
        }
        try {
            int proxyPort = Integer.parseInt(System.getProperty("http.proxyPort"));
            this.setProxyPort(proxyPort);
        }
        catch (NumberFormatException ex) {
            /* use default */
        }
        String proxyDomain = System.getProperty("http.proxyDomain");
        if (proxyDomain != null) {
            this.setProxyUsername(proxyDomain);
        }
        String proxyUser = System.getProperty("http.proxyUser");
        if (proxyUser != null) {
            this.setProxyUsername(proxyUser);
        }
        String proxyPassword = System.getProperty("http.proxyPassword");
        if (proxyPassword != null) {
            this.setProxyUsername(proxyPassword);
        }
        
    }
}