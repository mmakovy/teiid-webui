/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.teiid.webui.client.widgets.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.teiid.webui.client.dialogs.UiEvent;
import org.teiid.webui.client.dialogs.UiEventType;
import org.teiid.webui.client.widgets.CheckableNameTypeRow;
import org.teiid.webui.share.Constants;

import com.google.gwt.cell.client.Cell.Context;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.SelectionModel;

/**
 * Composite for display of Table names.  Contains checkboxes next to the rows which fire event
 * when the checkbox state is changed.
 */
public class TableNamesTable extends Composite {

	@Inject Event<UiEvent> uiEvent;
	
	private static String COLUMN_HEADER_NAME = "Tables";
	private static int TABLE_HEIGHT_PX = 200;
	private static int TABLE_WIDTH_PX = 280;
	private static int TABLE_VISIBLE_ROWS = 7;

    protected VerticalPanel panel = new VerticalPanel();
    protected Label label = new Label();

    private SimpleTable<CheckableNameTypeRow> table;
    private CheckboxHeader cbHeader;
    private String owner;
    private boolean showHeader = true;
    private boolean disableUncheckedRows = false;
    private DisableableCheckboxCell checkboxCell = new DisableableCheckboxCell(this.disableUncheckedRows,true,false);
    private Widget tablePanel;
    
    public TableNamesTable( ) {
        initWidget( panel );
        tablePanel = createTablePanel();
        panel.add(tablePanel);
    }
    
    public void setOwner(String owner) {
    	this.owner = owner;
    }
    
    public void setShowHeader(boolean showHeader) {
    	if(showHeader!=this.showHeader) {
    		this.showHeader = showHeader;
    		
    		// Remove current table
    		panel.remove(tablePanel);
    		
    		// Re-init
    		tablePanel = createTablePanel();
    		panel.add(tablePanel);
    	}
    }
    
    public void setDisableUncheckedRows(boolean disableUnchecked) {
    	this.checkboxCell.setDisableIfUnchecked(disableUnchecked);
    	table.redraw();
    }
    
    public void setCheckedState(String tableName, boolean isChecked) {
    	List<CheckableNameTypeRow> rows = table.getRowData();
    	for(CheckableNameTypeRow row : rows) {
    		if(row.getName().equalsIgnoreCase(tableName)) {
    			row.setChecked(isChecked);
    			break;
    		}
    	}
    }
    
    /**
     * Create the panel
     * @return the panel widget
     */
    protected Widget createTablePanel() {
    	table = new SimpleTable<CheckableNameTypeRow>(TABLE_HEIGHT_PX,TABLE_WIDTH_PX,TABLE_VISIBLE_ROWS);
    	
        // Add Checkbox column
    	Column<CheckableNameTypeRow, Boolean> checkboxColumn = new Column<CheckableNameTypeRow, Boolean>(checkboxCell)
    			{
    		@Override
    		public Boolean getValue(CheckableNameTypeRow object)
    		{
    			if(object == null) return false;
    			return object.isChecked();
    		}
    	};
    	checkboxColumn.setFieldUpdater(new FieldUpdater<CheckableNameTypeRow, Boolean>() {
    	    public void update(int index, CheckableNameTypeRow object, Boolean value) {
    	    	object.setChecked(value);
    	    	
    	    	boolean allRowsSame = true;
            	List<CheckableNameTypeRow> tableRows = table.getRowData();
            	boolean firstState = false;
            	for(int i=0; i<tableRows.size(); i++) {
            		CheckableNameTypeRow row = tableRows.get(i);
            		if(i==0) {
            			firstState = row.isChecked();
            		} else {
            			boolean thisState = row.isChecked();
            			if(thisState!=firstState) {
            				allRowsSame = false;
            				break;
            			}
            		}
            	}
            	if(allRowsSame) {
            		cbHeader.setValue(firstState);
            	} else {
            		cbHeader.setValue(false);
            	}
        		table.redrawHeaders();
       			fireCheckboxEvent(object.getName(),value);
    	    }
    	});

    	// Checkbox Header
        cbHeader = createCBHeader(false);

        if(showHeader) {
        	table.addColumn(checkboxColumn, cbHeader);
        } else {
            table.addColumn(checkboxColumn);
        }
    	table.setColumnWidth(checkboxColumn, 40, Unit.PX);
    		        
        // --------------
    	// Name Column
    	// --------------
        TextColumn<CheckableNameTypeRow> nameColumn = new TextColumn<CheckableNameTypeRow>() {
            public String getValue( CheckableNameTypeRow row ) {
                return row.getName();
            }
        };
        if(showHeader) {
            table.addColumn( nameColumn, COLUMN_HEADER_NAME );
        } else {
            table.addColumn( nameColumn );
        }
        table.setColumnWidth(nameColumn, 200, Unit.PX);
        
        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.add(table);
        return verticalPanel;
    }

	/*
	 * Fires Ui event when a table checkbox is checked or unchecked
	 */
	private void fireCheckboxEvent(String tableName,boolean isChecked) {
		UiEvent theEvent = null;
		if(isChecked) {
			theEvent = new UiEvent(UiEventType.TABLE_NAME_TABLE_CHECKBOX_CHECKED);
		} else {
			theEvent = new UiEvent(UiEventType.TABLE_NAME_TABLE_CHECKBOX_UNCHECKED);
		}
		theEvent.setEventSource(this.owner);
		theEvent.setTableName(tableName);
		uiEvent.fire(theEvent);
	}
    
    private CheckboxHeader createCBHeader(boolean isChecked) {
    	CheckboxHeader cbHeader = new CheckboxHeader(new CheckboxCell(),false) {
    		@Override
    		protected void headerUpdated(boolean checkState){
    			List<CheckableNameTypeRow> tableRows = table.getRowData();
    			for(CheckableNameTypeRow aRow : tableRows) {
    				aRow.setChecked(checkState);
    			}
    			UiEvent theEvent = new UiEvent(UiEventType.TABLE_NAME_TABLE_CHECKBOX_CHECKED);
    			theEvent.setEventSource(owner);
    			uiEvent.fire(theEvent);
    			table.redraw();
    		}
    	};
    	return cbHeader;
    }
    
    public void clear() {
    	setData(Collections.<CheckableNameTypeRow>emptyList());
    }
    
    public String getSelectedRowString() {
    	StringBuilder sb = new StringBuilder();
    	
    	List<CheckableNameTypeRow> rows = table.getRowData();
    	for(CheckableNameTypeRow row : rows) {
    		if(row.isChecked()) {
    			if(!sb.toString().isEmpty()) {
    				sb.append(Constants.COMMA);
    			}
    			sb.append(row.getName());
    		}
    	}
    	
    	return sb.toString();
    }
    
    public List<String> getSelectedTableNames() {
    	List<String> colNames = new ArrayList<String>();
    	
    	List<CheckableNameTypeRow> rows = table.getRowData();
    	for(CheckableNameTypeRow row : rows) {
    		if(row.isChecked() && row.getName()!=null) {
    			colNames.add(row.getName());
    		}
    	}
    	
    	return colNames;
    }
    public List<String> getSelectedTableTypes() {
    	List<String> colTypes = new ArrayList<String>();
    	
    	List<CheckableNameTypeRow> rows = table.getRowData();
    	for(CheckableNameTypeRow row : rows) {
    		if(row.isChecked() && row.getType()!=null) {
    			colTypes.add(row.getType());
    		}
    	}
    	
    	return colTypes;
    }
    
    public void setData(List<CheckableNameTypeRow> rows) {
    	// Resets table rows
    	table.setRowData(rows);
    	
    	// Header checkbox initially unchecked
    	cbHeader.setValue(false);
    	table.redrawHeaders();
    }
    
    public List<CheckableNameTypeRow> getData() {
    	return table.getRowData();
    }
    
	public void setSelectionModel( final SelectionModel<CheckableNameTypeRow> selectionModel ) {
        table.setSelectionModel( selectionModel );
    }
	
	/**
	 * Checkbox Header
	 */
	private class CheckboxHeader extends Header<Boolean> {
		Boolean checkedState = false;
		
	    public CheckboxHeader(CheckboxCell cell, boolean isChecked) {
	        super(cell);
	        checkedState = isChecked;
	    }
	    
	    public void setValue(boolean isChecked) {
	    	checkedState = isChecked;
	    }
	    
	    @Override
	    public Boolean getValue() {
	    	return checkedState;
	    }
	 
	    @Override
	    public void onBrowserEvent(Context context, Element elem, NativeEvent event) {
	        InputElement input = elem.getFirstChild().cast();
	        checkedState = input.isChecked();
	        headerUpdated(checkedState);
	    }
	    
	    protected void headerUpdated(boolean isChecked) {
	    	// override this method in defining class
	    }
	}
}
