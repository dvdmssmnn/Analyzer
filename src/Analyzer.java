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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateException;
import java.util.Base64;

import org.apache.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

public class Analyzer {
	private static Logger log = Logger.getLogger(Analyzer.class);

	// private static String host = "http://10.0.0.42:8080";

	private static String host = "http://10.0.0.5:8080";

	public static void main(String[] args) throws MalformedURLException,
			IOException, CertificateException {
		String dbURL = String.format("%s/cmd?command=download", host);
		String filename = "tmp.sqlite";

		BufferedInputStream in = null;
		FileOutputStream fout = null;
		try {
			in = new BufferedInputStream(new URL(dbURL).openStream());
			fout = new FileOutputStream(filename, false);

			final byte data[] = new byte[1024];
			int count;
			while ((count = in.read(data, 0, 1024)) != -1) {
				fout.write(data, 0, count);
			}
		} finally {
			if (in != null) {
				in.close();
			}
			if (fout != null) {
				fout.close();
			}
			log.info("Downloaded db");
		}

		File f = new File("certificates");
		if (f.exists()) {
			if (f.isDirectory()) {
				f.delete();
			}
		}
		f.mkdir();

		String certificatesURL = String.format(
				"%s/cmd?command=trusted_certificates", host);

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

			for (int i = 0; i < certificateArray.size(); ++i) {
				String base64 = certificateArray.get(i).getAsString();
				byte[] derData = Base64.getDecoder().decode(base64);

				File certFile = new File(
						String.format("certificates/%d.der", i));
				FileOutputStream out = new FileOutputStream(certFile);
				out.write(derData);
				out.close();
			}
		} finally {
			if (in != null) {
				in.close();
			}
			log.info("");
		}
	}
}
