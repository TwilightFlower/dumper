package io.github.nuclearfarts.dumper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JvmProcess implements Comparable<JvmProcess> {
	public final long pid;
	public final String target; // jar or specified main class
	public JvmProcess(long pid, String target) {
		this.pid = pid;
		this.target = target;
	}
	
	public static List<JvmProcess> getRunningJvms() {
		List<JvmProcess> procs = ProcessHandle.allProcesses().filter(p -> {
			ProcessHandle.Info info = p.info();
			if(info.command().isPresent() && info.arguments().isPresent()) {
				String cmd = info.command().get();
				return cmd.endsWith("java") || cmd.endsWith("javaw") || cmd.endsWith("java.exe") || cmd.endsWith("javaw.exe");
			}
			return false;
		}).map(p -> {
			ProcessHandle.Info info = p.info();
			String[] args = info.arguments().get();
			String target = "";
			boolean nextArgIsJar = false;
			int cpCounter = -1;
			// find target
			for(String arg : args) {
				if(arg.startsWith("-X") || arg.startsWith("-D")) {
					continue;
				}
				cpCounter--;
				if(nextArgIsJar) {
					target = arg;
					nextArgIsJar = false;
					break;
				} else if(cpCounter == 0) {
					target = arg;
					break;
				} else if("-jar".equals(arg)) {
					nextArgIsJar = true;
				} else if("-cp".equals(arg) || "-classpath".equals(arg)) {
					cpCounter = 2;
				}
			}
			return new JvmProcess(p.pid(), target);
		}).collect(Collectors.toList());
		Collections.sort(procs);
		return procs;
	}

	@Override
	public int compareTo(JvmProcess o) {
		return (int) (pid - o.pid);
	}
	
	@Override
	public int hashCode() {
		return (int) (pid * target.hashCode());
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof JvmProcess)) return false;
		JvmProcess o = (JvmProcess) other;
		return pid == o.pid && target.equals(o.target);
	}
}
