package de.keithpaterson.groundnet_checker;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mockit.Mock;
import mockit.MockUp;

public class TestOfTest {
	
	boolean called = false;

	@BeforeEach
	public void reset() {called = false;}
	
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

}
