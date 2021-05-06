package io.github.nuclearfarts.dumper.agent;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.ProtectionDomain;

public class Agent implements ClassFileTransformer {
	private final MappedByteBuffer commBuf;
	private final Instrumentation inst;
	private Path dumpDir;
	private String currentClassToDump;
	
	public static void agentmain(String args, Instrumentation inst) {
		System.out.println("[ClassDumper] Agent attached. Using comm file path: " + args);
		Path commFile = Path.of(args);
		Agent agent = new Agent(commFile, inst);
		inst.addTransformer(agent, true);
		Thread t = new Thread(() -> {
			while(true) {
				agent.loop();
			}
		}, "Class dumper thread");
		t.setDaemon(true);
		t.start();
	}
	
	private Agent(Path commFile, Instrumentation inst) {
		this.inst = inst;
		try {
			FileChannel channel = FileChannel.open(commFile, StandardOpenOption.READ, StandardOpenOption.WRITE);
			commBuf = channel.map(MapMode.READ_WRITE, 0, 2048);
		} catch (IOException e) {
			System.err.println("[ClassDumper] Encountered fatal exception");
			throw new RuntimeException(e);
		}
	}
	
	public void loop() {
		while(commBuf.get(0) != 1) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { }
		}
		commBuf.rewind();
		commBuf.get();
		int classNameLen = commBuf.getInt();
		byte[] strBytes = new byte[classNameLen];
		commBuf.get(strBytes);
		String className = new String(strBytes, StandardCharsets.UTF_8);
		int dumpLocLen = commBuf.getInt();
		byte[] dumpLocBytes = new byte[dumpLocLen];
		commBuf.get(dumpLocBytes);
		String dumpLoc = new String(dumpLocBytes, StandardCharsets.UTF_8);
		System.out.println("[ClassDumper] Dumping " + className + " to " + dumpLoc);
		dumpDir = Paths.get(dumpLoc);
		currentClassToDump = className.replace('.', '/');
		for(Class<?> c : inst.getAllLoadedClasses()) {
			if(className.equals(c.getName())) {
				try {
					System.out.println("[ClassDumper] Found class " + c.getName());
					inst.retransformClasses(c);
				} catch (UnmodifiableClassException e) {
					System.err.println("[ClassDumper] Class " + className + " is unmodifiable");
					e.printStackTrace();
				}
				break;
			}
		}
		commBuf.put(0, (byte) 0);
	}
	
	public byte[] transform(ClassLoader loader, String name, Class<?> clazz, ProtectionDomain domain, byte[] classBytes) {
		if(name.equals(currentClassToDump)) {
			try(OutputStream out = new BufferedOutputStream(Files.newOutputStream(dumpDir.resolve(name.replace('/', '.') + ".class")))) {
				out.write(classBytes);
			} catch (IOException e) {
				System.err.println("[ClassDumper] Error attempting to dump class " + name);
				e.printStackTrace();
			}
		}
		return classBytes;
	}
}
