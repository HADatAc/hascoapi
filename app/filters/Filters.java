package filters;

import org.pac4j.play.filters.SecurityFilter;
import play.filters.cors.CORSFilter;
import play.http.HttpFilters;
import play.mvc.EssentialFilter;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class Filters implements HttpFilters {

    private final SecurityFilter securityFilter;
    private final CORSFilter corsFilter;

    @Inject
    public Filters(SecurityFilter securityFilter, CORSFilter corsFilter) {
        this.securityFilter = securityFilter;
        this.corsFilter = corsFilter;
    }

    @Override
    public List<EssentialFilter> getFilters() {
        return Arrays.asList(corsFilter.asJava(), securityFilter.asJava());
    }
}
