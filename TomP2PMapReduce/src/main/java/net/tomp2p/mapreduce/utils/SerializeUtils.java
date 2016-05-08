package net.tomp2p.mapreduce.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * This class provides methods to serialize and deserialize both class files and java objects. It is used by {@link Job}
 * to serialise and transport user-defined {@link Task} and {@link IMapReduceBroadcastReceiver} implementations. As
 * users define these classes themselves on one node, other nodes do not know of their implementation. Using the
 * provided methods in this class, it is possible to transfer and instantiate defined objects and classes on nodes that
 * do not know of their implementation. See also {@link TransferObject}, which is used to store and transfer serialised
 * objects and classes.
 * 
 * @author Oliver Zihler
 *
 */
public class SerializeUtils {

	/**
	 * Serialises a class (its class file) and all contained declared or anonymous classes. Starts with the uppermost
	 * declaring class of the class provided as parameter. Allows sending unknown classes to other computers.See
	 * {@link #deserializeClassFiles(Map)} to see how these classes can be defined on a node again. See also
	 * {@link TransferObject} for an easy aggregation of objects and corresponding class files.
	 * 
	 * 
	 * @param classToSerialize
	 *            the class (file) that should be serialised for transfer
	 * @return a {@link Map} containing all classes specified by there name as key and the value the corresponding
	 *         byte[] containing the serialised class.
	 * @throws IOException
	 */
	public static Map<String, byte[]> serializeClassFile(Class<?> classToSerialize) throws IOException {
		Map<String, byte[]> visitor = new TreeMap<>();
		// Check if class is declared inside another class. If so, start serialization from parent
		while (classToSerialize.getDeclaringClass() != null) { // serialises everything
			classToSerialize = classToSerialize.getDeclaringClass();
		}
		// Class<?> parent = classToSerialize.getDeclaringClass();
		// while (parent != null) {
		// visitor.put(parent.getName(), toByteArray(parent.getName()));
		// parent = parent.getDeclaringClass()
		// }
		// Only serialises direct parents
		// Serialize down the tree
		internalSerialize(classToSerialize, visitor);
		return visitor;
	}

	private static void internalSerialize(Class<?> classToSerialize, Map<String, byte[]> visitor) {
		byte[] byteArray = toByteArray(classToSerialize.getName());
		visitor.put(classToSerialize.getName(), byteArray);
		findAnonymousClasses(visitor, classToSerialize.getName());
		// Get all declared inner classes, interfaces, and so on.
		for (Class<?> clazz : classToSerialize.getDeclaredClasses()) {
			internalSerialize(clazz, visitor);
		}

	}

	protected static void findAnonymousClasses(Map<String, byte[]> visitor, String classToSerializeName) {

		int lastIndexOf$ = classToSerializeName.lastIndexOf("$");
		String lastNumber = classToSerializeName.substring(lastIndexOf$ + 1, classToSerializeName.length());
		if (lastIndexOf$ == -1 || notFollowedByNumber(lastNumber)) {
			// This is the initial class name .
			classToSerializeName = classToSerializeName + "$" + 1;
			findAnonymousClasses(visitor, classToSerializeName);
		} else {
			// increment the class name and look for the next class
			byte[] byteArray = toByteArray(classToSerializeName);
			if (byteArray == null) {
				classToSerializeName = classToSerializeName.substring(0, classToSerializeName.lastIndexOf("$"));

				int lastIndexOfPrevious$ = classToSerializeName.lastIndexOf("$");
				lastNumber = classToSerializeName.substring(lastIndexOfPrevious$ + 1, classToSerializeName.length());
				if (lastIndexOfPrevious$ == -1 || notFollowedByNumber(lastNumber)) {
					return; // Back at initial classpath
				} else {
					String count = classToSerializeName.substring(lastIndexOfPrevious$ + 1,
							classToSerializeName.length());
					int newCounter = Integer.parseInt(count);
					++newCounter; // Increment it for the next round
					classToSerializeName = classToSerializeName.substring(0, lastIndexOfPrevious$ + 1) + newCounter;
					findAnonymousClasses(visitor, classToSerializeName);
				}
			} else {
				visitor.put(classToSerializeName, byteArray);
				classToSerializeName = classToSerializeName + "$" + 1;
				findAnonymousClasses(visitor, classToSerializeName);
			}
		}

	}

	private static boolean notFollowedByNumber(String convertToNumber) {
		if (convertToNumber == null || convertToNumber.trim().length() == 0) {
			return true;
		}
		try {
			Integer.parseInt(convertToNumber);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	private static byte[] toByteArray(String c) {
		try {
			// c.getName looks lik this: mapreduce.execution.jobs.Job
			// System.err.println(c);
			InputStream is = SerializeUtils.class.getResourceAsStream(c + ".class");
			if (is == null) {
				is = SerializeUtils.class.getResourceAsStream("/" + c.replace(".", "/") + ".class");
				// System.err.println("1" + is);
			}
			if (is == null) {
				is = SerializeUtils.class.getResourceAsStream(c.replace(".", "/") + ".class");
				// System.err.println("2" + is);
			}

			if (is == null) {
				return null;
			}

			ByteArrayOutputStream buffer = new ByteArrayOutputStream();

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				buffer.write(data, 0, nRead);
			}

			buffer.flush();

			return buffer.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Deserialises and loads unknown classes such that they are available on a computer that received them. See
	 * {@link #serializeClassFile(Class)} to see how a class can be serialised for transfer.
	 * 
	 * @param classesToDefine
	 *            {@link Map} containing all class names as Strings (key) and corresponding class files as byte[]
	 *            (value)
	 * @return {@link Map} containing the actual {@link Class} files. To be used in e.g.
	 *         {@link #deserializeJavaObject(byte[], Map)}
	 */
	public static Map<String, Class<?>> deserializeClassFiles(Map<String, byte[]> classesToDefine) {
		ByteClassLoader l = new ByteClassLoader(ClassLoader.getSystemClassLoader(), classesToDefine);
		Map<String, Class<?>> classes = new HashMap<>();
		for (String className : classesToDefine.keySet()) {
			try {
				Class<?> c = l.findClass(className);
				classes.put(className, c);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return classes;
	}

	/**
	 * Deserialises a Java object. To be able to use it, first of all, the corresponding class files need to be
	 * deserialised using {@link #deserializeClassFiles(Map)}, which need to be provided as a parameter.
	 * 
	 * @param objectData
	 *            the serialised Java object to deserialise. See {@link #serializeJavaObject(Object)}.
	 * @param classes
	 *            the deserialised Java classes needed to instantiate the serialised object. See
	 *            {@link #deserializeClassFiles(Map)}
	 * @return the deserialised Java object
	 */
	public static Object deserializeJavaObject(byte[] objectData, Map<String, Class<?>> classes) {
		Object object = null;
		try {
			ByteObjectInputStream objectStream = new ByteObjectInputStream(new ByteArrayInputStream(objectData),
					classes);
			object = objectStream.readObject();
			objectStream.close();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return object;
	}

	/**
	 * Serialises a Java object. See {@link #deserializeJavaObject(byte[], Map). See also {@link TransferObject} for an
	 * easy aggregation of objects and corresponding class files.
	 * 
	 * @param object
	 *            the Java object to serialise
	 * @return a serialised Java object as byte[] that can be used for transfer.
	 */
	public static byte[] serializeJavaObject(Object object) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try {
			ObjectOutput out = new ObjectOutputStream(bos);
			out.writeObject(object);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] objectInBytes = bos.toByteArray();
		return objectInBytes;
	}

	/**
	 * Internal class used to resolve class files
	 * 
	 * @author Oliver Zihler
	 *
	 */
	private static class ByteObjectInputStream extends ObjectInputStream {
		private Map<String, Class<?>> classes;

		public ByteObjectInputStream(InputStream in, Map<String, Class<?>> classes) throws IOException {
			super(in);
			this.classes = classes;
		}

		@Override
		protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
			Class<?> c = classes.get(desc.getName());
			if (c != null) {
				return c;
			}
			return super.resolveClass(desc);
		}

	}
}
