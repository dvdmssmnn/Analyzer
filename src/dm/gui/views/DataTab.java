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

package dm.gui.views;

import java.io.FileNotFoundException;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import dm.analyze.ISearchable;
import dm.analyze.SensitiveInformation;
import dm.util.Strings;

public class DataTab extends Tab {

	private String dataPath;
	private ListView<String> searchItemsListView = new ListView<String>();
	private ListView<ISearchable> searchResultsListView = new ListView<ISearchable>();

	public DataTab(String dir) throws FileNotFoundException {
		super("Data");
		setClosable(false);
		this.dataPath = String.format("%s/data.json", dir);

		HBox hbox = new HBox();
		this.setContent(hbox);

		hbox.getChildren().add(searchItemsListView);
		hbox.getChildren().add(this.searchResultsListView);

		this.searchResultsListView.setMinWidth(300);

		HBox.setHgrow(this.searchResultsListView, Priority.ALWAYS);
		this.initSearchItemsListView();
	}

	private void initSearchItemsListView() throws FileNotFoundException {
		ObservableList<String> list = FXCollections.observableArrayList();

		for (String s : SensitiveInformation.getSearchItems(dataPath)) {
			list.add(Strings.hexToString(s));
		}

		this.searchItemsListView.setItems(list);

		this.searchItemsListView.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<String>() {

					@Override
					public void changed(ObservableValue<? extends String> arg0,
							String arg1, String arg2) {
						if (arg2 != null) {
							DataTab.this.updateResults(Strings
									.stringToHex(arg2));
						}
					}
				});
	}

	private void initSearchResultsListView() {
		ObservableList<ISearchable> items = FXCollections.observableArrayList();

		this.searchResultsListView.setItems(items);
	}

	private void updateResults(String hexData) {
		this.searchResultsListView.getItems().clear();

		for (ISearchable result : ISearchable.find(hexData).keySet()) {
			this.searchResultsListView.getItems().add(result);
		}
	}
}
