package module;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class OnStart {

    private static final Logger log = LoggerFactory.getLogger(OnStart.class);

    @Inject
    public OnStart() {
        initDirectoryStructure();

    }

    private void initDirectoryStructure() {
        List<String> listFolderPaths = new LinkedList<String>();
		/*listFolderPaths.add("tmp");
		listFolderPaths.add("logs");
		listFolderPaths.add("processed_csv");
		listFolderPaths.add("unprocessed_csv");
		listFolderPaths.add("downloaded_csv");
		listFolderPaths.add("working_csv");
		listFolderPaths.add("tmp/ttl");
		listFolderPaths.add("tmp/cache");
		listFolderPaths.add("tmp/uploads");*/

    }
}

