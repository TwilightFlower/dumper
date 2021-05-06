package io.github.nuclearfarts.dumper;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import io.github.nuclearfarts.dumper.agent.Agent;

public class AgentConnection {
	private static final Map<JvmProcess, AgentConnection> CONNECTIONS = new HashMap<>();
	private static final long MY_PID = ProcessHandle.current().pid();
	private static final String AGENT_PATH = getAgentPath().toAbsolutePath().toString();
	
	private final MappedByteBuffer commBuf;
	
	private AgentConnection(JvmProcess process) {
		try {
			Path commFile = Files.createTempFile(MY_PID + "_" + process.pid, null);
			String commFileLoc = commFile.toAbsolutePath().toString();
			FileChannel channel = FileChannel.open(commFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.READ, StandardOpenOption.WRITE);
			commBuf = channel.map(MapMode.READ_WRITE, 0, 2048);
			System.out.println("Attempting attach to " + process.pid);
			VirtualMachine vm = VirtualMachine.attach(Long.toString(process.pid));
			vm.loadAgent(AGENT_PATH, commFileLoc);
			System.out.println("Attached.");
		} catch (IOException | AttachNotSupportedException | AgentLoadException | AgentInitializationException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void askDump(String clazz, String to) {
		commBuf.rewind();
		System.out.println("Asking for dump");
		byte[] classBytes = clazz.getBytes(StandardCharsets.UTF_8);
		byte[] toBytes = to.getBytes(StandardCharsets.UTF_8);
		commBuf.put((byte) 1);
		commBuf.putInt(classBytes.length);
		System.out.println("Wrote name len: " + classBytes.length);
		commBuf.put(classBytes);
		commBuf.putInt(toBytes.length);
		commBuf.put(toBytes);
		commBuf.put(0, (byte) 1);
		System.out.println(commBuf);
	}
	
	public void awaitDump() throws InterruptedException {
		while(commBuf.get(0) != 0) {
			Thread.sleep(50);
		}
	}
	
	public static AgentConnection get(JvmProcess process) {
		return CONNECTIONS.computeIfAbsent(process, AgentConnection::new);
	}
	
	private static Path getAgentPath() {
		try {
			return Path.of(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
