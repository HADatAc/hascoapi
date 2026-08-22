package module;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hascoapi.Constants;
import org.hascoapi.RepositoryInstance;
import org.hascoapi.entity.pojo.StreamTopic;
import org.hascoapi.utils.NameSpaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hascoapi.utils.ConfigProp;


@Singleton
public class OnStart {

    private static final Logger log = LoggerFactory.getLogger(OnStart.class);

    @Inject
    public OnStart() {
		System.out.println("[OnStart] Step 1: Initializing directory structure...");
		runStartupStep("initDirectoryStructure", () -> initDirectoryStructure());

		System.out.println("[OnStart] Step 2: Getting RepositoryInstance...");
		runStartupStep("RepositoryInstance.getInstance", () -> RepositoryInstance.getInstance());

		System.out.println("[OnStart] Step 3: Updating local namespace...");
		runStartupStep("NameSpaces.updateLocalNamespace", () -> NameSpaces.getInstance().updateLocalNamespace());

		System.out.println("[OnStart] Step 4: Initiating StreamTopics...");
		runStartupStep("StreamTopic.initiateStreamTopics", () -> StreamTopic.initiateStreamTopics());

		System.out.println("[OnStart] Step 5: Startup complete (with possible degraded services if warnings were printed). ");
    }

	@FunctionalInterface
	private interface StartupAction {
		void run() throws Exception;
	}

	private void runStartupStep(String stepName, StartupAction action) {
		try {
			action.run();
		} catch (Throwable t) {
			System.err.println("[OnStart] WARNING: startup step failed [" + stepName + "]: " + t.getMessage());
			t.printStackTrace();
			log.warn("Startup step failed [{}]", stepName, t);
		}
	}

    private void initDirectoryStructure() {
        List<String> listFolderPaths = new LinkedList<String>();
        listFolderPaths.add(ConfigProp.getPathIngestion());
		if (ConfigProp.getPathIngestion().endsWith("/")) {
			listFolderPaths.add(ConfigProp.getPathIngestion() + Constants.RESOURCE_FOLDER);
		} else {
			listFolderPaths.add(ConfigProp.getPathIngestion());
		}
        listFolderPaths.add(ConfigProp.getPathAppOntology());
		/*listFolderPaths.add("tmp");
		listFolderPaths.add("logs");
		listFolderPaths.add("processed_csv");
		listFolderPaths.add("unprocessed_csv");
		listFolderPaths.add("downloaded_csv");
		listFolderPaths.add("working_csv");
		listFolderPaths.add("tmp/ttl");
		listFolderPaths.add("tmp/cache");
		listFolderPaths.add("tmp/uploads");*/

		for(String path : listFolderPaths){
			File folder = new File(path);
			// if the directory does not exist, create it
			if (!folder.exists()) {
				System.out.println("creating directory: " + path);
				try{
					folder.mkdir();
				} 
				catch(SecurityException se){
					System.out.println("Failed to create directory.");
				}
				System.out.println("DIR created");
			}
		}
    }
}

