package it.eng.msp.core.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Utility class to manage file facilities, mainly dealing with basic CRUD
 * operations on file system (such as copy, read, write).
 * 
 * @author Angelo Marguglio <br>
 *         Company: Engineering Ingegneria Informatica S.p.A. <br>
 *         E-mail: <a href="mailto:angelo.marguglio@eng.it">angelo.marguglio@eng.it</a>
 */
public class FileUtils {

	public final static String PATH_SEPARATOR = "_"; 

	public final static String WEBPATH_SEPARATOR = "/"; 

	public final static String FILESTORE = "/var/www/img/cashma/";

	public final static String IMG_JPG = ".jpg";

	public static final String USER_HOME = System.getProperty("user.home");

	/**
	 * Copy all the content from the source to the destination, replacing all
	 * the target content.
	 * 
	 * @param in
	 *            - The input file used as source
	 * @param out
	 *            - The output file used as destination
	 */
	public static void copyfile(InputStream in, OutputStream out) {
		copyfile(in, out, true);
	}

	/**
	 * Copy all the content from the source to the destination, replacing all
	 * the target content.
	 * 
	 * @param in
	 *            - The input file used as source
	 * @param out
	 *            - The output file used as destination
	 * @param closeFiles
	 *            - The flag to indicate if the files have to be closed
	 */
	public static void copyfile(InputStream in, OutputStream out,
			boolean closeFiles) {
		try {
			// //For Append the file.
			// // OutputStream out = new FileOutputStream(f2,true);
			// //For Overwrite the file.
			// OutputStream out = new FileOutputStream(f2);

			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}

			if (closeFiles) {
				in.close();
				out.close();
			}
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a new file in the given path with the given content.
	 * 
	 * @param filePath
	 *            - The path for the output file to be created
	 * @param content
	 *            - The content to be written
	 */
	public static void writeToFile(String filePath, String content) {
		try {
			File outputFile = new File(filePath);
			outputFile.createNewFile();

			OutputStream out = new FileOutputStream(outputFile);
			out.write(content.trim().getBytes());
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param path
	 *            - the full path to be parsed
	 * @return The last segment of the given path, containing only the file
	 *         name.
	 */
	public static String getFileName(String path) {
		return getFileName(path, false);
	}

	/**
	 * @param path
	 *            - the full path to be parsed
	 * @param removeNumbers
	 *            - force removing version numbers
	 * @return The last segment of the given path, containing only the file
	 *         name.
	 */
	public static String getFileName(String path, boolean removeNumbers) {
		String fileName = path.substring(path.lastIndexOf("/") + 1);

		// Get the index of the last "."
		int lastDot = fileName.lastIndexOf(".");

		// Read the file extension
		String ext = (lastDot > 0) ? fileName.substring(lastDot) : "";

		// Remove, if it exists, the extension
		if ((lastDot > 0))
			fileName = fileName.substring(0, lastDot);

		// Remove all the information about the version number
		if(removeNumbers)
			fileName = fileName.replaceAll("\\.", "").replaceAll("-0", "")
			.replaceAll("-1", "").replaceAll("-2", "").replaceAll("-3", "")
			.replaceAll("-4", "").replaceAll("-5", "").replaceAll("-6", "")
			.replaceAll("-7", "").replaceAll("-8", "").replaceAll("-9", "")
			.replaceAll("0", "").replaceAll("1", "").replaceAll("2", "")
			.replaceAll("3", "").replaceAll("4", "").replaceAll("5", "")
			.replaceAll("6", "").replaceAll("7", "").replaceAll("8", "")
			.replaceAll("9", "");

		return fileName + ext;
	}
	
	public static File createTempFile(String name, byte[] content) throws Exception {
		try {
			File file = File.createTempFile(name, null);
			file.deleteOnExit();

			BufferedOutputStream os = new BufferedOutputStream(
					new FileOutputStream(file));

			os.write(content);
			os.close();
			return file;
		} catch (IOException e) {
			throw new Exception("Cannot create temp file for " + name, e);
		}
	}

	/**
	 * Create a temporary JAR file starting from the given parameters.
	 * 
	 * @param name
	 *            - the name of the generated file
	 * @param jarFile
	 *            - the input file to be read
	 * @param jarEntry
	 *            - the entry of the read file to be used
	 * @return
	 * @throws Exception
	 */
	public static File createTempJarFile(String name, JarFile jarFile,
			JarEntry jarEntry) throws Exception {
		try {
			File file = File.createTempFile(name + ".", ".jar");
			file.deleteOnExit();

			BufferedOutputStream os = new BufferedOutputStream(
					new FileOutputStream(file));

			os.write(getJarBytes(jarFile, jarEntry));
			os.close();
			return file;
		} catch (IOException e) {
			throw new Exception("Cannot create temp file for " + name, e);
		}
	}

	/**
	 * Read the given entry from the given JAR file.
	 * 
	 * @param jarFile
	 *            - the JAR file to be read
	 * @param jarEntry
	 *            - the entry to be read
	 * @return
	 * @throws Exception
	 */
	private static byte[] getJarBytes(JarFile jarFile, JarEntry jarEntry)
			throws Exception {
		DataInputStream dis = null;
		byte[] a_by = null;
		try {
			long lSize = jarEntry.getSize();
			if (lSize <= 0 || lSize >= Integer.MAX_VALUE) {
				throw new Exception("Invalid size " + lSize + " for entry "
						+ jarEntry);
			}
			a_by = new byte[(int) lSize];
			InputStream is = jarFile.getInputStream(jarEntry);
			dis = new DataInputStream(is);
			dis.readFully(a_by);
		} catch (IOException e) {
			throw e;
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException e) {
				}
			}
		}
		return a_by;
	} // getJarBytes()

	public static byte[] toByteArray(String path) {
		return toByteArray(new File(path), false);
	}
	
	public static byte[] toByteArray(String path, boolean delete) {
		return toByteArray(new File(path), delete);
	}
	
	public static byte[] toByteArray(File file){
		return toByteArray(file, false);
	}

	public static byte[] toByteArray(File file, boolean delete) {
		if(file==null) return null;
		// La lunghezza del file può eccedere i limiti del tipo int
		if (file.length() > Integer.MAX_VALUE)
			throw new IndexOutOfBoundsException("File troppo grande per essere salvato in memoria");

		try {
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();


			int dataSize;
			byte buf[] = new byte[4096];//4000

			try {
				while((dataSize = fis.read(buf)) != -1) {
					baos.write(buf, 0, dataSize);
				}    
			} finally {
				if(fis != null)
					fis.close();
				if(delete)file.delete();
			}
			return baos.toByteArray();
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	public static File fromByteArray(String path, byte[] in) throws IOException {
		return fromByteArray(new File(path), in);
	}

	public static byte[] toByteArrayP(File file) {
		if(file==null) return null;
		try {

			byte [] res = new byte[(int)file.length()];
			DataInputStream dis = new DataInputStream(new FileInputStream(file));
			dis.readFully(res);
			dis.close();

			return res;
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	public static File fromByteArrayP(File out, byte[] in) throws IOException {
		FileOutputStream fos = new FileOutputStream(out);
		DataOutputStream dos = new DataOutputStream(fos);
		dos.write(in);
		dos.close();

		return out;
	}

	public static File fromByteArray(File out, byte[] in) throws IOException {
		if(out!=null && out.getParentFile()!=null && !out.exists())
			out.getParentFile().mkdirs();

		FileOutputStream fos = new FileOutputStream(out);
		fos.write(in);
		fos.close();

		return out;
	}

	public static byte[] toByteArray(Blob blob) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			return toByteArrayImpl(blob, baos);
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}

	public static byte[] toByteArrayImpl(Blob blob, 
			ByteArrayOutputStream baos) throws SQLException, IOException {
		byte buf[] = new byte[4000];
		int dataSize;
		InputStream is = blob.getBinaryStream(); 

		try {
			while((dataSize = is.read(buf)) != -1) {
				baos.write(buf, 0, dataSize);
			}    
		} finally {
			if(is != null) {
				is.close();
			}
		}
		return baos.toByteArray();
	}

	public static String getSystemTmpDir() {
		return System.getProperty("java.io.tmpdir");
	}

	public static String createFolder(String father, String child) {
		if(father==null || child==null)
			return null;

		father = FileUtils.checkPath(father);
		String path = father + child;
		new File(path).mkdir();
		return path;
	}

	/**
	 * Legge il contenuti di tutti i files all'interno della directory passata
	 * come parametro e li ritorna al chiamante all'interno di una List<byte[]>.
	 * Legge i file da disco in ordine alfabetico.
	 * */
	public static List<byte[]> readFilesFromDisk(String dirName) {
		File dir = new File(dirName);
		File[] filenames = dir.listFiles();
		Arrays.sort(filenames);

		List<byte[]> files = new LinkedList<byte[]>();
		for(File f : filenames)
			files.add(FileUtils.toByteArray(f));

		return files;
	}

	public static Map<String, byte[]> readFilesFromDiskAsMap(String dirName) {
		File dir = new File(dirName);
		File[] filenames = dir.listFiles();
		Arrays.sort(filenames);

		Map<String, byte[]> files = new LinkedHashMap<String, byte[]>();
		for(File f : filenames)
			files.put(f.getName(), FileUtils.toByteArray(f));

		return files;
	}

	public static int getFileListSize(String dirName) {
		File dir = new File(dirName);
		return dir.list().length;
	}

	public static byte[] zipDir(String dir) throws Exception {
		return zipDir(dir, false);
	}

	public static byte[] zipDir(String dir, boolean removeRoot) throws Exception {
		File zipFile = null;
		try {
			String root = null;
			if(removeRoot)
				root = dir;

			zipFile = File.createTempFile("out", ".zip");

			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));

			File dirObj = new File(dir);
			addDir(root, dirObj, out);
			out.close();

			return toByteArray(zipFile);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if(zipFile!=null)
				zipFile.deleteOnExit();
		}
	}

	private static void addDir(String root, File dirObj, ZipOutputStream out) throws IOException {
		File[] files = dirObj.listFiles();
		byte[] tmpBuf = new byte[1024];

		for (int i = 0; i < files.length; i++) {
			if (files[i].isDirectory()) {
				addDir(root, files[i], out);
				continue;
			}
			FileInputStream in = new FileInputStream(files[i].getAbsolutePath());
			if(root!=null)
				out.putNextEntry(new ZipEntry(files[i].getAbsolutePath().replaceAll(root, "")));
			else
				out.putNextEntry(new ZipEntry(files[i].getAbsolutePath()));

			int len;
			while ((len = in.read(tmpBuf)) > 0) {
				out.write(tmpBuf, 0, len);
			}
			out.closeEntry();
			in.close();
		}
	}

	public static void unzipDir(String folder, byte[] in) {
		folder = checkPath(folder);

		ZipInputStream zipinputstream = null;
		ZipEntry zipentry;

		File inFile = null;

		try {
			inFile = File.createTempFile("input", ".zip");
			fromByteArray(inFile, in);

			byte[] buf = new byte[1024];
			zipinputstream = new ZipInputStream(new FileInputStream(inFile));
			zipentry = zipinputstream.getNextEntry();
			while (zipentry != null) {
				String entryName = zipentry.getName();
				FileOutputStream fileoutputstream;
				File newFile = new File(entryName);
				String directory = newFile.getParent();
				File directoryFile = new File(folder + directory);

				if (directory == null) {
					if (newFile.isDirectory())
						break;
				} else {
					if(!directoryFile.exists())
						directoryFile.mkdirs();
				}

				fileoutputstream = new FileOutputStream(folder + entryName);

				int n;
				while ((n = zipinputstream.read(buf, 0, 1024)) > -1){
					fileoutputstream.write(buf, 0, n);
				}
				fileoutputstream.close();
				zipinputstream.closeEntry();
				zipentry = zipinputstream.getNextEntry();
			}
			zipinputstream.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(inFile!=null)
				inFile.deleteOnExit();
		}
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(String path) {
		return clearTempData(new File(path));
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(File dir) {
		return clearTempData(dir, false);
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @param force - Forza la cancellazione
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(String path, boolean force) {
		return clearTempData(new File(path), force);
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @param force - Forza la cancellazione
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(File dir, boolean force) {
		if(force) {
			try {
				if (dir.isDirectory()) {
					String[] content = dir.list();
					for (int i = 0; i < content.length; i++)
						clearTempData(new File(dir, content[i]), force);
				}
				return dir.delete();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}

		return true;
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @param extension - L'estensione dei file da ricercare
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(String path, String extension) {
		return clearTempData(new File(path), extension);
	}

	/**
	 * Cancella ogni copia temporanea dei file utilizzati durante l'elaborazione
	 * 
	 * @param dir - La directory da cancellare
	 * @param extension - L'estensione dei file da ricercare
	 * @return true in caso di successo, false altrimenti.
	 */
	public static boolean clearTempData(File dir, String extension) {
		if(!dir.isDirectory()) {
			return false;
		} 

		for(File f : dir.listFiles()) {
			if(f.isDirectory() || !f.getName().endsWith(extension))
				continue;

			f.delete();
		}

		return true;
	}

	public static String getParentFolder(String filename) throws IOException {
		int index = filename.lastIndexOf(File.separator);
		return filename.substring(0, index + 1);
	}

	public static String getBasename(String filename) throws IOException {
		int index = filename.lastIndexOf(File.separator);
		return filename.substring(index + 1);
	}

	public static byte[] hexToBytes(char[] hex) {
		int length = hex.length / 2;
		byte[] raw = new byte[length];
		for (int i = 0; i < length; i++) {
			int high = Character.digit(hex[i * 2], 16);
			int low = Character.digit(hex[i * 2 + 1], 16);
			int value = (high << 4) | low;
			if (value > 127)
				value -= 256;
			raw[i] = (byte) value;
		}
		return raw;
	}

	public static String checkPath(String in) {
		if(in==null) return in;

		return in.endsWith(File.separator) ? in : in + File.separator;
	}

	public static String pathJoin(String... pathElements) {
		StringBuffer joined = new StringBuffer();
		for(String element : pathElements) {
			joined.append(element);

			if(!element.endsWith(File.separator))
				joined.append(File.separator);
		}

		// Rimozione ultimo separatore
		int len = joined.length();
		joined = joined.delete(len - 1, len);
		return joined.toString();
	}

	public static String relativizeWithURI(String path, String base) {
		return new File(base).toURI().relativize(new File(path).toURI()).getPath();
	}

	public static String relativize(String path, String base) {
		String count = "/..";
		String tmp = base.substring(base.indexOf(File.separator)+1);
		while(tmp.contains(File.separator)) {
			tmp = tmp.substring(tmp.indexOf(File.separator)+1);
			count += "/..";
		}

		return base + count + path;
	}

	private static final long ONE_KB = 1024;
	private static final long ONE_MB = ONE_KB * ONE_KB;
	private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;

	public static void copyFile(File srcFile, File destFile) throws IOException {
		if (srcFile == null) {
			throw new NullPointerException("Source must not be null");
		}
		if (destFile == null) {
			throw new NullPointerException("Destination must not be null");
		}
		if (srcFile.exists() == false) {
			throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
		}
		if (srcFile.isDirectory()) {
			throw new IOException("Source '" + srcFile + "' exists but is a directory");
		}
		if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
			throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
		}
		File parentFile = destFile.getParentFile();
		if (parentFile != null) {
			if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
				throw new IOException("Destination '" + parentFile + "' directory cannot be created");
			}
		}
		if (destFile.exists() && destFile.canWrite() == false) {
			throw new IOException("Destination '" + destFile + "' exists but is read-only");
		}
		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel input = null;
		FileChannel output = null;

		try{
			fis = new FileInputStream(srcFile);
			fos = new FileOutputStream(destFile);
			input  = fis.getChannel();
			output = fos.getChannel();
			long size = input.size();
			long pos = 0;
			long count = 0;
			while (pos < size) {
				count = size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : size - pos;
				pos += output.transferFrom(input, pos, count);
			}
		} finally {
			output.close();
			fos.close();
			input.close();
			fis.close();
		}
	}
}

