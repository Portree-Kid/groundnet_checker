package de.keithpaterson.groundnet_checker;

import mockit.Mock;
import mockit.MockUp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.ClassNameFilter.includeClassNamePatterns;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;

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

		System.setProperty("TRAVIS_BRANCH", "GROUNDNET_EGEO_123");
		launcher.execute(request);

		assertEquals(1, listener.getResults().size());
		System.out.println(request);
	}

	@Test
	public void testListenerNotBranch() {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectPackage("de.keithpaterson.groundnet_checker"), selectClass(GroundNetTest.class))
				.filters(includeClassNamePatterns(".*GroundNetTest")).build();

		Launcher launcher = LauncherFactory.create();

		// Register a listener of your choice
		GroundnetTestExecutionListener listener = new GroundnetTestExecutionListener();
		launcher.registerTestExecutionListeners(listener);

		System.setProperty("TRAVIS_BRANCH", "TOWER_EGEO_123");
		launcher.execute(request);

		assertEquals(7, listener.getResults().size());
		for (Result result: listener.getResults().values()) {
			assertEquals(TestExecutionResult.Status.SUCCESSFUL, result.getTestExecutionResult().getStatus());
		}
		System.out.println(request);
	}

	@Test
	public void testListenerMainBranch() {
		LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
				.selectors(selectPackage("de.keithpaterson.groundnet_checker"), selectClass(GroundNetTest.class))
				.filters(includeClassNamePatterns(".*Gr.*Test")).build();

		Launcher launcher = LauncherFactory.create();

		// Register a listener of your choice
		GroundnetTestExecutionListener listener = new GroundnetTestExecutionListener();
		launcher.registerTestExecutionListeners(listener);

		System.setProperty("TRAVIS_BRANCH", "master");
		launcher.execute(request);

		assertEquals(99, listener.getResults().size());
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
