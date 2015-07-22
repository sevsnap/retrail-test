package it.eng.msp.core.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Class di utilita' per zip ed unzip di file
 *
 */
public class ZipUtils {

//	/**
//	 * Crea l'archivio zip a partire dalla inFolder 
//	 * 
//	 * @param inFolder
//	 * @param zipFile
//	 * @throws IOException
//	 */
//	public static void zip(File inFolder, File zipFile) throws IOException {
//		zip(inFolder, zipFile, inFolder.getName());
//	}
	
	/**
	 * Crea un archivio zip a partire dal file inFile
	 * 
	 * @param inFile
	 * @param zipFile
	 * @param rootZipFolderName
	 * @throws IOException
	 */
	public static void zip(File inFile, File zipFile) throws IOException {
		if(inFile!=null) {
			if(inFile.isDirectory()) {
				zip(inFile, zipFile, inFile.getName());
			} else {
				ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zipFile));
				zip(inFile, "/", z);
				z.close();
			}
		} else
			throw new IOException("Impossibile creare l'archivio, la directory selezionata e' vuota");
	}
	
	/**
	 * Crea un archivio zip a partire dalla infolder, 
	 * sostituisce nello zip il nome della infolder 
	 * con quello di rootZipFolderName
	 * 
	 * @param inFolder
	 * @param zipFile
	 * @param rootZipFolderName
	 * @throws IOException
	 */
	public static void zip(File inFolder, File zipFile, String rootZipFolderName) throws IOException {
		String[] dirlist = inFolder.list();

		if(dirlist!=null) {
			ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zipFile));

			for(int i=0; i<dirlist.length; i++){
				zip(new File(inFolder.getPath() + "/" + dirlist[i]), 
						rootZipFolderName + "/", 
						z);			
			}

			z.close();
		} else
			throw new IOException("Impossibile creare l'archivio, la directory selezionata e' vuota");
	}


	private static void zip(File input, String destDir, ZipOutputStream z)
			throws IOException {
		if (!input.isDirectory()) {
			z.putNextEntry(new ZipEntry((destDir + input.getName())));

			FileInputStream inStream = new FileInputStream(input);
			byte[] a = new byte[(int) input.length()];
			int did = inStream.read(a);

			if (did != input.length())
				throw new IOException("Impossibile leggere tutto il file "
						+ input.getPath() + " letti solo " + did + " di "
						+ input.length());

			z.write(a, 0, a.length);

			z.closeEntry();
			inStream.close();
			input = null;
		} else { // recurse
			String newDestDir = destDir + input.getName() + "/";
			String newInpurPath = input.getPath() + "/";

			z.putNextEntry(new ZipEntry(newDestDir));
			z.closeEntry();

			String[] dirlist = (input.list());
			input = null;

			for (int i = 0; i < dirlist.length; i++)
				zip(new File(newInpurPath + dirlist[i]), newDestDir, z);
		}
	}

	/**
	 * Estrae il contenuto del file zip in outFolder
	 * 
	 * @param zipFile
	 * @param outFolder
	 * @throws IOException
	 */
	public static void unzip(File zipFile, File outFolder) throws IOException {
		BufferedOutputStream out = null;
		ZipInputStream in = new ZipInputStream(
				new BufferedInputStream(
						new FileInputStream(zipFile)));
		ZipEntry entry;
		
		while ((entry = in.getNextEntry()) != null) {
			if (entry.isDirectory()) {
				File d = new File(outFolder.getPath() + entry.getName()); // "/" + entry.getName());
				d.mkdirs();
			} else {
				int count;
				byte data[] = new byte[1000];

				// write the files to the disk
				out = new BufferedOutputStream(new FileOutputStream(outFolder.getPath()), 1000); // outFolder.getPath() + "/" + entry.getName()), 1000);

				while ((count = in.read(data, 0, 1000)) != -1) {
					out.write(data, 0, count);
				}

				out.flush();
				out.close();
			}
		}

		in.close();
	}

}
