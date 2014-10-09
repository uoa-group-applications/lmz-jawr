package nz.ac.auckland.lmz.jawr;

import com.bluetrainsoftware.classpathscanner.ClasspathScanner;
import com.bluetrainsoftware.classpathscanner.ResourceScanListener;
import net.jawr.web.resource.bundle.factory.util.ConfigPropertiesSource;
import nz.ac.auckland.lmz.flags.Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
public class JawrConfig implements ConfigPropertiesSource {
	protected Logger log =  LoggerFactory.getLogger(getClass());

	protected JawrResourceScanListener resourceScanListener;

	Properties jawrProperties = new Properties();
	protected boolean scanned = false;

	class JawrResourceScanListener implements ResourceScanListener {
		Properties config = new Properties();
		List<ResourceScanListener.ScanResource> resources = new ArrayList<ResourceScanListener.ScanResource>();

		@Override
		public List<ResourceScanListener.ScanResource> resource(List< ResourceScanListener.ScanResource > scanResources) throws Exception {
			scanned = true; // the scanner has run at least once, so when getConfigProperties is called further down we know not to do it again

			resources.clear();

			for (ResourceScanListener.ScanResource scanResource : scanResources) {
				if ("META-INF/jawr.properties".equals(scanResource.resourceName) ||
					"WEB-INF/jawr.properties".equals(scanResource.resourceName) ||
					"META-INF/resources/WEB-INF/jawr.properties".equals(scanResource.resourceName)) {
					resources.add(scanResource);
				}
			}

			return resources;
		}

		@Override
		public void deliver(ResourceScanListener.ScanResource desire, InputStream inputStream) {
			Properties properties = new Properties();
			try {

				properties.load(inputStream);
			} catch (IOException e) {
				log.error("Failed to load {}", desire.getResolvedUrl().toString());
			}

			config.putAll(properties);
		}

		@Override
		public ResourceScanListener.InterestAction isInteresting(ResourceScanListener.InterestingResource
		interestingResource) {
			return InterestAction.REPEAT;
		}

		@Override
		public void scanAction(ScanAction action) {
			if (action == ScanAction.STARTING) {
				config = new Properties();
			} else if (action == ScanAction.COMPLETE) {
				jawrProperties = config;
			}
		}
	}

	public JawrConfig() {
		resourceScanListener = new JawrResourceScanListener();

		ClasspathScanner.getInstance().registerResourceScanner(resourceScanListener);
	}

	@Override
	public Properties getConfigProperties() {
		if (Flags.DEVMODE.on() || !scanned) {
			ClasspathScanner.getInstance().scan(getClass().getClassLoader(), true);
		}

		return jawrProperties;
	}

	@Override
	public boolean configChanged() {
		return Flags.DEVMODE.on();
	}
}