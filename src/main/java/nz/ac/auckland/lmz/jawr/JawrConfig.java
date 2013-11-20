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
	protected static Logger log =  LoggerFactory.getLogger(JawrConfig.class);

	protected JawrResourceScanListener resourceScanListener;

	static class JawrResourceScanListener implements ResourceScanListener {
		Properties config = new Properties();
		List<ResourceScanListener.ScanResource> resources = new ArrayList<>();

		@Override
		public List<ResourceScanListener.ScanResource> resource(List< ResourceScanListener.ScanResource > scanResources) throws Exception {
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
	}

	public JawrConfig() {
		resourceScanListener = new JawrResourceScanListener();

		ClasspathScanner.getInstance().registerResourceScanner(resourceScanListener);
	}

	@Override
	public Properties getConfigProperties() {
		if (Flags.DEVMODE.on()) {
			resourceScanListener.config.clear();
			ClasspathScanner.getInstance().scan(getClass().getClassLoader(), true);
		}

		return resourceScanListener.config;
	}

	@Override
	public boolean configChanged() {
		return Flags.DEVMODE.on();
	}
}
