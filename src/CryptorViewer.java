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

import java.io.File;
import java.nio.file.Paths;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TabPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import dm.db.DBHelper;
import dm.gui.views.CryptoTab;
import dm.gui.views.DataTab;

public class CryptorViewer extends Application {

	private TabPane rootPane;

	private String dir;

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		DirectoryChooser dirChoose = new DirectoryChooser();
		dirChoose.setInitialDirectory(Paths.get("").toAbsolutePath().toFile());
		File dir = dirChoose.showDialog(primaryStage);
		if (dir == null) {
			System.exit(0);
		}
		this.dir = dir.toString();
		DBHelper.setDBPath(String.format("%s/db.sqlite", dir.getAbsolutePath()));

		this.rootPane = new TabPane();

		DataTab dataTab = new DataTab(this.dir);
		this.rootPane.getTabs().add(new CryptoTab());
		this.rootPane.getTabs().add(dataTab);
		primaryStage.setScene(new Scene(this.rootPane));
		primaryStage.setMaximized(true);
		primaryStage.show();
		primaryStage.setTitle(this.dir);

	}

}
