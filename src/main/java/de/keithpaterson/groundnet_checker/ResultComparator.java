package de.keithpaterson.groundnet_checker;

import java.util.Comparator;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.junit.platform.launcher.TestIdentifier;

public class ResultComparator implements Comparator<Entry<TestIdentifier, Result>> {
	Pattern p = Pattern.compile("([^\\s]+)");

	@Override
	public int compare(Entry<TestIdentifier, Result> o1, Entry<TestIdentifier, Result> o2) {
		String s1 = o1.getKey().getDisplayName().split("#")[0].split("\\.")[0];
		String s2 = o2.getKey().getDisplayName().split("#")[0].split("\\.")[0];
		return s1.compareTo(s2);
	}

}
