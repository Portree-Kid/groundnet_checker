package de.keithpaterson.groundnet_checker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

public class GroundnetTestExecutionListener implements TestExecutionListener {

	static Hashtable<TestIdentifier, Result> results = new Hashtable<>();

	static int testCount = 0;

	private static Boolean exported = Boolean.FALSE;

	private HashMap<String, Traffic> trafficList;

	@SuppressWarnings("unchecked")
	private void loadTraffic() {
		try (ObjectInputStream is = new ObjectInputStream(new FileInputStream("traffic.obj"))) {
			trafficList = (HashMap<String, Traffic>) is.readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void testPlanExecutionStarted(TestPlan testPlan) {
		results.clear();
		loadTraffic();
		TestExecutionListener.super.testPlanExecutionStarted(testPlan);
		TestIdentifier rootTest = testPlan.getRoots().toArray(new TestIdentifier[1])[0];
		System.out.println("Roottest : " + rootTest + " " + results.size());
	}

	@Override
	public void dynamicTestRegistered(TestIdentifier testIdentifier) {
		TestExecutionListener.super.dynamicTestRegistered(testIdentifier);
		results.put(testIdentifier, new Result());
		testCount++;
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		// TODO Auto-generated method stub
		TestExecutionListener.super.executionSkipped(testIdentifier, reason);
		System.out.println("executionSkipped");
	}

	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		// TODO Auto-generated method stub
		TestExecutionListener.super.executionStarted(testIdentifier);
//		System.out.println("executionStarted");
	}

	@Override
	public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
		// TODO Auto-generated method stub
		TestExecutionListener.super.reportingEntryPublished(testIdentifier, entry);
//		System.out.println("reportingEntryPublished");
	}

	@Override
	public void testPlanExecutionFinished(TestPlan testPlan) {
		// TODO Auto-generated method stub
		TestExecutionListener.super.testPlanExecutionFinished(testPlan);
//		System.out.println("testPlanExecutionFinished " + testPlan.toString());
		if (testPlan.getClass().getName().contains("InternalTestPlan")) {
//			System.out.println(testCount);
			if (testCount == 0) {
				synchronized (exported) {
					if (!exported) {
						System.out.println("All Done");
						exported = Boolean.TRUE;
						exportHTML(results);
					}
				}
			}
		}
	}

	private void exportHTML(Hashtable<TestIdentifier, Result> results2) {
		File f = new File("target/site/index.html");
		f.getParentFile().mkdirs();
		System.out.println(f.getAbsolutePath());
		try (PrintStream fos = new PrintStream(new FileOutputStream(f))) {

			fos.print("<html>\r\n");
			List<Entry<TestIdentifier, Result>> l = new ArrayList<Entry<TestIdentifier, Result>>();
			l.addAll(results2.entrySet());
			Collections.sort(l, new ResultComparator());
			List<Entry<TestIdentifier, Result>> filteredList = l.stream()
					.filter(c -> trafficList.get(c.getKey().getDisplayName().split("#")[0].split("\\.")[0]) != null
							&& trafficList.get(c.getKey().getDisplayName().split("#")[0].split("\\.")[0])
									.getFlights() > 0)
					.collect(Collectors.toList());
			fos.print("<TABLE>\r\n");
			fos.print("<TH>ICAO</TH><TH>Result</TH><TH>Tests</TH><TH>Errors</TH><TH>Flights</TH>\r\n");
			String lastIcao = "";
			PrintStream fos2 = null;

			int failures = 0;
			int tests = 0;
			for (Entry<TestIdentifier, Result> entry : filteredList) {
				String icao = entry.getKey().getDisplayName().split("#")[0].split("\\.")[0];
				TestExecutionResult testExecutionResult = entry.getValue().getTestExecutionResult();
				if (!icao.equals(lastIcao)) {
					if (!lastIcao.isEmpty()) {
						if (fos2 != null) {
							fos2.print("</TABLE>\r\n");
							fos2.print("</html>\r\n");
							fos2.close();
						}
						fos.print("<TR>\r\n");
						fos.print("<TD><A href=\"" + lastIcao + ".html\">"+ lastIcao + "</TD>");
						fos.print("<TD>" + (failures>0?"FAILED":"SUCCESS") + "</TD>");
						fos.print("<TD>" + tests + "</TD>");
						fos.print("<TD>" + failures + "</TD>");
						fos.print("<TD>" + trafficList.get(icao).getFlights() + "</TD>");
						fos.print("</TR>\r\n");
					}

					File f2 = new File("target/site/" + icao + ".html");
					fos2 = new PrintStream(new FileOutputStream(f2));
					fos2.print("<html>\r\n");
					fos2.print("<H2>" + icao + "</H2>");
					fos2.print("<TABLE>\r\n");
					failures = 0;
					tests = 0;
					lastIcao = icao;
				}
				tests++;
				if (testExecutionResult.getStatus().equals(Status.FAILED)) {
					failures++;
					Throwable cause = testExecutionResult.getThrowable().get();
					String[] msgs = StringEscapeUtils.escapeHtml4(cause.getMessage().split("==>")[0]).split("[|]+");
					for (String msg : msgs) {
						fos2.print("<TR><TD>" + msg + "</TD></TR>");						
					}
				}
				System.out.println(icao + "\t" + testExecutionResult);
			}
			if(!lastIcao.isEmpty()) {
				fos.print("<TR>\r\n");
				fos.print("<TD><A href=\"" + lastIcao + ".html\">"+ lastIcao + "</TD>");
				fos.print("<TD>" + (failures>0?"FAILED":"SUCCESS") + "</TD>");
				fos.print("<TD>" + tests + "</TD>");
				fos.print("<TD>" + failures + "</TD>");
				fos.print("<TD>" + trafficList.get(lastIcao).getFlights() + "</TD>");
				fos.print("</TR>\r\n");				
			}
			fos.print("</html>\r\n");
			System.out.println("DONE");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		TestExecutionListener.super.executionFinished(testIdentifier, testExecutionResult);
		if (results.containsKey(testIdentifier)) {
			testCount = testCount - 1;
			results.get(testIdentifier).setTestExecutionResult(testExecutionResult);
//			System.out.println("executionFinished " + testExecutionResult.toString());
		}
//		System.out.println("executionFinished " + testIdentifier.toString() + "\t" + testExecutionResult.toString());
	}

	public static Hashtable<TestIdentifier, Result> getResults() {
		return results;
	}

}
