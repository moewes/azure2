package net.moewes.app.oidc.logging;

import com.microsoft.azure.storage.StorageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import net.moewes.Dao;
import net.moewes.app.oidc.AuthRequest;
import net.moewes.app.oidc.AuthRequestsBean;
import net.moewes.app.oidc.ConfigurationBean;

import java.net.URISyntaxException;
import java.time.LocalDateTime;

@ApplicationScoped
public class LogBean {

    @Inject
    ConfigurationBean configBean;

    @Inject
    AuthRequestsBean authRequestsBean;

    @Inject
    Dao<LogEntry> dao;

    public void log(String text, UriInfo uriInfo) {

        LocalDateTime time = LocalDateTime.now();
        LogEntry entry = new LogEntry("OIDC",time.toString());
        entry.setPath(text);

        if (uriInfo!=null){
            AuthRequest authRequest = authRequestsBean.extractRequestParameter(uriInfo);
            entry.setState(authRequest.getState());
            entry.setNonce(authRequest.getNonce());
        }

        if (configBean.hasDbLog()) {
            try {
                dao.save(entry);
            } catch (URISyntaxException | StorageException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
