package de.embl.cba.tables.annotate;

import de.embl.cba.tables.SwingUtils;
import de.embl.cba.tables.color.CategoryTableRowColumnColoringModel;
import de.embl.cba.tables.color.ColorUtils;
import de.embl.cba.tables.color.SelectionColoringModel;
import de.embl.cba.tables.select.SelectionModel;
import de.embl.cba.tables.tablerow.TableRow;
import ij.gui.GenericDialog;
import net.imglib2.type.numeric.ARGBType;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Annotator < T extends TableRow > extends JFrame
{
	private final String annotationColumnName;
	private final List< T > tableRows;
	private final SelectionModel< T > selectionModel;
	private final CategoryTableRowColumnColoringModel< T > coloringModel;
	private final SelectionColoringModel< T > selectionColoringModel;
	private final RowSorter< ? extends TableModel > rowSorter;
	private final JPanel panel;
	private boolean skipNone;
	private boolean isSingleRowBrowsingMode = false; // TODO: think about how to get out of this mode!
	private int selectedRowIndex = 0;

	public Annotator(
			String annotationColumnName,
			List< T > tableRows,
			SelectionModel< T > selectionModel,
			CategoryTableRowColumnColoringModel< T > coloringModel,
			SelectionColoringModel< T > selectionColoringModel,
			RowSorter< ? extends TableModel > rowSorter )
	{
		super("");
		this.annotationColumnName = annotationColumnName;
		this.tableRows = tableRows;
		this.selectionModel = selectionModel;
		this.coloringModel = coloringModel;
		this.selectionColoringModel = selectionColoringModel;
		this.rowSorter = rowSorter;
		coloringModel.fixedColorMode( true );
		this.panel = new JPanel();
	}

	public void showDialog()
	{
		addAnnotationButtons();
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addCreateCategoryButton();
		panel.add( new JSeparator( SwingConstants.HORIZONTAL ) );
		addTableRowBrowserPanel();
		addSkipNonePanel();
		showFrame();
	}

	private void showFrame()
	{
		this.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		panel.setOpaque( true ); //content panes must be opaque
		panel.setLayout( new BoxLayout( panel, BoxLayout.Y_AXIS ) );
		this.setContentPane( panel );
		this.setLocation( MouseInfo.getPointerInfo().getLocation().x, MouseInfo.getPointerInfo().getLocation().y );
		this.pack();
		this.setVisible( true );
	}

	private void addCreateCategoryButton()
	{
		final JButton button = new JButton( "Create new category" );
		panel.add( button );
		button.addActionListener( e -> {
			final GenericDialog gd = new GenericDialog( "" );
			gd.addStringField( "Category name", "", 10 );
			gd.showDialog();
			if ( gd.wasCanceled() ) return;
			addAnnotationButtonPanel( gd.getNextString(), null );
			refreshDialog();
		} );
	}

	private void addAnnotationButtons()
	{
		final HashMap< String, T > annotations = getAnnotations();
		for ( String annotation : annotations.keySet() )
			addAnnotationButtonPanel( annotation, annotations.get( annotation ) );
	}

	private void addAnnotationButtonPanel( String annotationName, T tableRow )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JButton button = new JButton( String.format("%1$15s", annotationName) );
		button.setFont( new Font("monospaced", Font.PLAIN, 12) );
		button.setOpaque( true );
		setButtonColor( button, tableRow );
		button.setAlignmentX( Component.CENTER_ALIGNMENT );

		final ARGBType argbType = new ARGBType();
		coloringModel.convert( annotationName, argbType );
		button.setBackground( ColorUtils.getColor( argbType ) );

		button.addActionListener( e -> {
			if ( selectionModel.isEmpty() ) return;

			final Set< T > selected = selectionModel.getSelected();

			for ( T row : selected )
			{
				row.setCell( annotationColumnName, annotationName );
			}

			if ( selected.size() > 1 )
				isSingleRowBrowsingMode = false;

			if( ! isSingleRowBrowsingMode )
			{
				selectionModel.clearSelection();
			}
			else
			{
				// Hack to notify all listeners that the coloring might have changed.
				selectionModel.clearSelection();
				selectionModel.setSelected( selected, true );
			}
		} );

		final JButton changeColor = new JButton( "C" );
		changeColor.addActionListener( e -> {
			Color color = JColorChooser.showDialog( this.panel, "", null );
			if ( color == null ) return;
			button.setBackground( color );
			coloringModel.putInputToFixedColor( annotationName, ColorUtils.getARGBType( color ) );
		} );

		panel.add( button );
		panel.add( changeColor );
		this.panel.add( panel );
	}

	private void addTableRowBrowserPanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JButton previous = new JButton( "Select previous" );
		previous.setFont( new Font("monospaced", Font.PLAIN, 12) );
		previous.setAlignmentX( Component.CENTER_ALIGNMENT );

		previous.addActionListener( e ->
		{
			isSingleRowBrowsingMode = true;

			int currentRowIndex = selectedRowIndex;
			if ( selectedRowIndex > 0 )
			{
				T row = null;
				if ( skipNone )
				{
					while ( selectedRowIndex > 0 )
					{
						row = tableRows.get( rowSorter.convertRowIndexToModel( --selectedRowIndex ) );
						if ( isNoneOrNan( row ) )
						{
							row = null;
							continue;
						}
						else
							break;
					}

					if ( row == null )
					{
						selectedRowIndex = currentRowIndex;
						return; // None of the previous rows is not None
					}
				}
				else
				{
					row = tableRows.get( rowSorter.convertRowIndexToModel( --selectedRowIndex ) );
				}

				selectRow( row );
			}
		} );

		final JButton next = new JButton( "Select next" );
		next.setFont( new Font("monospaced", Font.PLAIN, 12) );
		next.setAlignmentX( Component.CENTER_ALIGNMENT );

		next.addActionListener( e ->
		{
			isSingleRowBrowsingMode = true;

			int currentRowIndex = selectedRowIndex;
			if ( selectedRowIndex < tableRows.size() - 1 )
			{
				T row = null;
				if ( skipNone )
				{
					while ( selectedRowIndex < tableRows.size() )
					{
						row = tableRows.get( rowSorter.convertRowIndexToModel( ++selectedRowIndex ) );
						if ( isNoneOrNan( row ) )
						{
							row = null;
							continue;
						}
						else
							break;
					}
					if ( row == null )
					{
						selectedRowIndex = currentRowIndex;
						return; // None of the next rows is not None
					}
				}
				else
				{
					row = tableRows.get( rowSorter.convertRowIndexToModel( ++selectedRowIndex ) );
				}

				selectRow( row );
			}
		} );

		panel.add( previous );
		panel.add( next );
		this.panel.add( panel );
	}

	private boolean isNoneOrNan( T row )
	{
		return row.getCell( annotationColumnName ).toLowerCase().equals( "none" )
			|| row.getCell( annotationColumnName ).toLowerCase().equals( "nan" );
	}

	private void selectRow( T row )
	{
		if ( ! row.getCell( annotationColumnName ).toLowerCase().equals( "none" ) )
		{
			selectionColoringModel.setSelectionColoringMode( SelectionColoringModel.SelectionColoringMode.OnlyShowSelected );
		}
		else
		{
			selectionColoringModel.setSelectionColoringMode( SelectionColoringModel.SelectionColoringMode.SelectionColor );
		}

		selectionModel.clearSelection();
		selectionModel.setSelected( row, true );
		selectionModel.focus( row );
	}

	private void addSkipNonePanel( )
	{
		final JPanel panel = SwingUtils.horizontalLayoutPanel();

		final JCheckBox checkBox = new JCheckBox( "Skip \"None\" & \"NaN\"" );
		checkBox.setSelected( true );
		skipNone = checkBox.isSelected();

		checkBox.addActionListener( e -> {
			skipNone = checkBox.isSelected();
		}  );

		panel.add( checkBox );
		this.panel.add( panel );
	}

	private void setButtonColor( JButton button, T tableRow )
	{
		if ( tableRow != null )
		{
			final ARGBType argbType = new ARGBType();
			coloringModel.convert( tableRow, argbType );
			button.setBackground( new Color( argbType.get() ) );
		}
	}

	private HashMap< String, T > getAnnotations()
	{
		final HashMap< String, T > annotationToTableRow = new HashMap<>();

		for ( int row = 0; row < tableRows.size(); row++ )
		{
			final T tableRow = tableRows.get( row );
			annotationToTableRow.put( tableRow.getCell( annotationColumnName ), tableRow );
		}

		return annotationToTableRow;
	}

	private void refreshDialog()
	{
		panel.revalidate();
		panel.repaint();
		this.pack();
	}
}
