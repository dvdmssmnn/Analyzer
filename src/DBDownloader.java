//Copyright (c) 2015, David Missmann
//All rights reserved.
//
//Redistribution and use in source and binary forms, with or without modification,
//are permitted provided that the following conditions are met:
//
//1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
//disclaimer.
//
//2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
//disclaimer in the documentation and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
//INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
//DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
//SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
//SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
//WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
//OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class DBDownloader {

	public static void main(String[] args) throws MalformedURLException,
			IOException, InterruptedException, CertificateException {
		if (args.length < 2) {
			System.err.println("call with <host> and <filename> as arguments");
			return;
		}

		String host = args[0];
		String dir = args[1];

		if (dir.endsWith("/")) {
			dir = dir.substring(0, dir.length() - 1);
		}

		File dbDir = new File(dir);
		dbDir.mkdirs();

		loadDB(host, dir);
		loadCertificates(host, dir);
		initDataJSON(dir);
	}

	private static void loadDB(String host, String dir) throws IOException,
			InterruptedException {
		String dbURL = String.format("%s/cmd?command=download", host);

		BufferedInputStream dbInputStream = new BufferedInputStream(new URL(
				dbURL).openStream());
		String path = String.format("%s/db.sqlite", dir);
		FileOutputStream dbOutputStream = new FileOutputStream(path, false);

		final byte data[] = new byte[1024];
		int count;
		while ((count = dbInputStream.read(data, 0, 1024)) != -1) {
			dbOutputStream.write(data, 0, count);
		}

		dbInputStream.close();
		dbOutputStream.close();

		Runtime.getRuntime().exec("sh checkdb.sh").waitFor();

		System.out.println("Database downloaded");
	}

	private static void loadCertificates(String host, String dir)
			throws IOException, CertificateException {
		String certificatesURL = String.format(
				"%s/cmd?command=trusted_certificates", host);
		BufferedInputStream in = null;
		try {
			in = new BufferedInputStream(new URL(certificatesURL).openStream());
			final byte data[] = new byte[1024];
			int count;
			StringBuffer buffer = new StringBuffer();
			while ((count = in.read(data, 0, 1024)) != -1) {
				String s = new String(data, 0, count);
				buffer.append(s);
			}
			JsonArray certificateArray = new JsonParser().parse(
					buffer.toString()).getAsJsonArray();

			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");

			for (int i = 0; i < certificateArray.size(); ++i) {
				String base64 = certificateArray.get(i).getAsString();
				byte[] derData = Base64.getDecoder().decode(base64);

				X509Certificate certificate = (X509Certificate) certificateFactory
						.generateCertificate(new ByteArrayInputStream(derData));

				File certFile = new File(String.format("%s/%s.der", dir,
						certificate.getSubjectDN().toString()));
				FileOutputStream out = new FileOutputStream(certFile);
				out.write(derData);
				out.close();
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

	private static void initDataJSON(String dir) throws IOException {
		String path = String.format("%s/data.json", dir);
		BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));
		writer.write("[]");
		writer.close();
	}
}
