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

import java.util.Comparator;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import dm.analyze.cipher.SymmetricCipher;
import dm.analyze.hash.FoundInHashWarning;
import dm.analyze.warningns.IWarning;
import dm.analyze.warningns.Warning;
import dm.data.Trace;
import dm.data.TraceCall;
import dm.util.Strings;

public class CryptoTab extends Tab {

	private ListView<SymmetricCipher> cipherListView = new ListView<SymmetricCipher>();
	private TextArea cipherPlaintextTextArea = new TextArea();
	private ListView<IWarning> warningsListView = new ListView<IWarning>();
	private TextArea warningInfoTextArea = new TextArea();
	private TreeView<String> traceTreeView = new TreeView<String>();

	private SymmetricCipher selectedCipher = null;

	public CryptoTab() {
		super("Crypto");

		this.initCipherListView();
		this.initWarningsListView();

		VBox vbox = new VBox();
		vbox.getChildren().add(warningInfoTextArea);
		vbox.getChildren().add(traceTreeView);

		HBox hbox = new HBox();
		hbox.getChildren().add(this.cipherListView);
		hbox.getChildren().add(this.cipherPlaintextTextArea);
		hbox.getChildren().add(this.warningsListView);
		hbox.getChildren().add(vbox);

		HBox.setHgrow(this.cipherListView, Priority.ALWAYS);
		HBox.setHgrow(this.cipherPlaintextTextArea, Priority.ALWAYS);
		HBox.setHgrow(this.warningsListView, Priority.ALWAYS);
		HBox.setHgrow(this.warningInfoTextArea, Priority.ALWAYS);

		ObservableList<SymmetricCipher> ciphers = FXCollections
				.observableArrayList();

		for (SymmetricCipher cipher : SymmetricCipher.getCiphers()) {
			ciphers.add(cipher);
		}

		ciphers.sort(new Comparator<SymmetricCipher>() {

			@Override
			public int compare(SymmetricCipher o1, SymmetricCipher o2) {
				return o1.getCallID() < o2.getCallID() ? -1
						: (o1.getCallID() > o2.getCallID() ? 1 : 0);
			}
		});

		this.setContent(hbox);
		this.setClosable(false);
	}

	private void initCipherListView() {
		ObservableList<SymmetricCipher> ciphers = FXCollections
				.observableArrayList();
		for (SymmetricCipher cipher : SymmetricCipher.getCiphers()) {
			ciphers.add(cipher);
		}
		this.cipherListView.setItems(ciphers);

		this.cipherListView.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<SymmetricCipher>() {

					@Override
					public void changed(
							ObservableValue<? extends SymmetricCipher> arg0,
							SymmetricCipher arg1, SymmetricCipher arg2) {
						CryptoTab.this.selectedCipher = arg2;

						CryptoTab.this.cipherPlaintextTextArea.setText(Strings
								.hexToString(arg2.getPlaintextData()));

						CryptoTab.this.updateWarningsListView();
					}
				});
	}

	private void initWarningsListView() {

		this.warningsListView
				.setCellFactory(new Callback<ListView<IWarning>, ListCell<IWarning>>() {

					@Override
					public ListCell<IWarning> call(ListView<IWarning> arg0) {
						ListCell<IWarning> cell = new WarningCell();
						return cell;
					}
				});

		this.warningsListView.getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<IWarning>() {

					@Override
					public void changed(
							ObservableValue<? extends IWarning> arg0,
							IWarning arg1, IWarning arg2) {
						if (arg2 == null) {
							CryptoTab.this.warningInfoTextArea.setText("");
							return;
						}
						CryptoTab.this.warningInfoTextArea.setText("");
						if (arg2 instanceof FoundInHashWarning) {
							FoundInHashWarning warning = (FoundInHashWarning) arg2;
							CryptoTab.this.warningInfoTextArea.setText(Strings
									.hexToString(warning.getInput()));

							return;
						}
						Object info = arg2.getValue(Warning.INFO);
						if (info != null) {
							CryptoTab.this.warningInfoTextArea
									.setText((String) info);
						}
						Object data = arg2.getValue(Warning.DATA);
						if (data != null) {
							CryptoTab.this.warningInfoTextArea.setText(Strings
									.hexToString((String) data));
						}

						Object trace = arg2.getValue(Warning.TRACE);
						if (trace != null) {
							CryptoTab.this.updateTraceView((Trace) trace);
						}
					}
				});
	}

	private void updateTraceView(Trace trace) {
		TreeItem<String> root = new TreeItem<String>(String.format("%s %s",
				trace.getRoot().getClazz(), trace.getRoot().getMethod()));
		this.traceTreeView.setRoot(root);
		updateTreeItem(root, trace.getRoot());
	}

	private void updateTreeItem(TreeItem<String> item, TraceCall call) {
		item.setExpanded(true);
		for (TraceCall c : call.getCalled()) {
			TreeItem<String> newItem = new TreeItem<String>(String.format(
					"%d %s %s", c.getID(), c.getClazz(), c.getMethod()));
			item.getChildren().add(newItem);
			updateTreeItem(newItem, c);
		}
	}

	private void updateWarningsListView() {
		this.warningsListView.getSelectionModel().clearSelection();
		if (this.selectedCipher == null) {
			return;
		}
		this.warningsListView.getItems().clear();

		this.selectedCipher.checkCipher();
		this.warningsListView.getItems().addAll(
				this.selectedCipher.getWarnings());

	}
}
