package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

import java.io.File;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import mockit.Mock;
import mockit.MockUp;
import mockit.Mocked;
import mockit.Tested;

public class TestOfTest {

	boolean called = false;

	@BeforeEach
	public void reset() {
		called = false;
	}

	@Test
	public void test1() {

		MockUp<Assertions> mockUp = new MockUp<Assertions>() {
			@Mock
			Object fail(String m) {
				called = true;
				return null;
			}
		};
		File projectBaseDir = new File("./src/test/resources/brokenchar.groundnet.xml");
		System.out.println(projectBaseDir.getAbsolutePath());
		new GroundNetLoadingTest().testLoad("", projectBaseDir.toPath());
		assertTrue(called);
	}

	@Test
	public void testListener() {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectPackage("de.keithpaterson.groundnet_checker"), selectClass(GroundNetTest.class))
				.filters(includeClassNamePatterns(".*Gr.*Test")).build();

		Launcher launcher = LauncherFactory.create();

		// Register a listener of your choice
		GroundnetTestExecutionListener listener = new GroundnetTestExecutionListener();
		launcher.registerTestExecutionListeners(listener);

		launcher.execute(request);

		System.out.println(request);

	}

	public final class FakeEntry extends MockUp<TestIdentifier> {
		@Mock
		public String getDisplayName() { return "GFPD.groundnet.xml  ";};
	}


	@Test
	public void test() {
		new FakeEntry();
//		new ResultComparator().compare(o1, o2);
	}

}
