
package util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExportarExcel {

    private static final String VERDE_OSCURO = "176B43";
    private static final String VERDE_CLARO = "E8F3EC";
    private static final String VERDE_TOTAL = "D5E9DC";
    private static final String GRIS_BORDE = "D9E2DC";

    // ============================================================
    // PRODUCTOS MAS VENDIDOS
    // ============================================================

    public static boolean exportarProductosMasVendidos(
            String rutaArchivo, List<Object[]> datos, String periodo) {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Productos Más Vendidos");

            CellStyle tituloStyle = crearEstiloTitulo(workbook);
            CellStyle subtituloStyle = crearEstiloSubtitulo(workbook);
            CellStyle fechaStyle = crearEstiloFecha(workbook);
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle cuerpoStyle = crearEstiloCuerpo(workbook);
            CellStyle cuerpoAlternoStyle = crearEstiloCuerpoAlterno(workbook);
            CellStyle numeroStyle = crearEstiloNumero(workbook);
            CellStyle monedaStyle = crearEstiloMoneda(workbook);
            CellStyle totalTextoStyle = crearEstiloTotalTexto(workbook);
            CellStyle totalNumeroStyle = crearEstiloTotalNumero(workbook);
            CellStyle totalMonedaStyle = crearEstiloTotalMoneda(workbook);

            String[] columnas = {
                "Producto",
                "Categoría",
                "Cantidad Vendida",
                "Total Recaudado (S/)"
            };

            // Titulo principal
            Row filaTitulo = sheet.createRow(0);
            filaTitulo.setHeightInPoints(30);

            Cell titulo = filaTitulo.createCell(0);
            titulo.setCellValue("BODEGA LA PERUANITA");
            titulo.setCellStyle(tituloStyle);

            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            // Subtitulo
            Row filaSubtitulo = sheet.createRow(1);
            Cell subtitulo = filaSubtitulo.createCell(0);
            subtitulo.setCellValue("REPORTE DE PRODUCTOS MÁS VENDIDOS");
            subtitulo.setCellStyle(subtituloStyle);

            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 3));

            // Periodo
            Row filaPeriodo = sheet.createRow(2);
            Cell celdaPeriodo = filaPeriodo.createCell(0);
            celdaPeriodo.setCellValue("Periodo: " + periodo);
            celdaPeriodo.setCellStyle(fechaStyle);

            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 3));

            // Fecha de generacion
            Row filaFecha = sheet.createRow(3);
            Cell celdaFecha = filaFecha.createCell(0);

            String fechaActual = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            );

            celdaFecha.setCellValue("Generado el: " + fechaActual);
            celdaFecha.setCellStyle(fechaStyle);

            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 3));

            // Encabezados
            Row headerRow = sheet.createRow(5);
            headerRow.setHeightInPoints(25);

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 6;

            for (Object[] fila : datos) {

                Row row = sheet.createRow(rowNum);

                Cell c1 = row.createCell(0);
                c1.setCellValue(String.valueOf(fila[0]));
                c1.setCellStyle(rowNum % 2 == 0
                        ? cuerpoAlternoStyle : cuerpoStyle);

                Cell c2 = row.createCell(1);
                c2.setCellValue(String.valueOf(fila[1]));
                c2.setCellStyle(rowNum % 2 == 0
                        ? cuerpoAlternoStyle : cuerpoStyle);

                Cell c3 = row.createCell(2);
                c3.setCellValue(((Number) fila[2]).doubleValue());
                c3.setCellStyle(numeroStyle);

                Cell c4 = row.createCell(3);
                c4.setCellValue(((Number) fila[3]).doubleValue());
                c4.setCellStyle(monedaStyle);

                rowNum++;
            }

            // Fila de totales
            int ultimaFilaDatos = rowNum - 1;
            Row filaTotal = sheet.createRow(rowNum);

            Cell textoTotal = filaTotal.createCell(0);
            textoTotal.setCellValue("TOTAL GENERAL");
            textoTotal.setCellStyle(totalTextoStyle);

            Cell cantidadTotal = filaTotal.createCell(2);
            cantidadTotal.setCellFormula(
                "SUM(C7:C" + (ultimaFilaDatos + 1) + ")"
            );
            cantidadTotal.setCellStyle(totalNumeroStyle);

            Cell recaudacionTotal = filaTotal.createCell(3);
            recaudacionTotal.setCellFormula(
                "SUM(D7:D" + (ultimaFilaDatos + 1) + ")"
            );
            recaudacionTotal.setCellStyle(totalMonedaStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(rowNum, rowNum, 0, 1)
            );

            // Configuracion de hoja
            configurarHoja(sheet, 4, 3, 5, rowNum);

            // Anchos de columnas
            sheet.setColumnWidth(0, 8500);
            sheet.setColumnWidth(1, 6500);
            sheet.setColumnWidth(2, 5500);
            sheet.setColumnWidth(3, 7000);

            // Calculo automatico de formulas
            workbook.setForceFormulaRecalculation(true);

            // Guardar
            try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (Exception e) {
            System.err.println(
                "Error al exportar productos más vendidos: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // METODOS DE PAGO
    // ============================================================

    public static boolean exportarMetodosPago(
            String rutaArchivo, List<Object[]> datos, String periodo) {

        try (Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet("Métodos de Pago");

            CellStyle tituloStyle = crearEstiloTitulo(workbook);
            CellStyle subtituloStyle = crearEstiloSubtitulo(workbook);
            CellStyle fechaStyle = crearEstiloFecha(workbook);
            CellStyle headerStyle = crearEstiloEncabezado(workbook);
            CellStyle cuerpoStyle = crearEstiloCuerpo(workbook);
            CellStyle cuerpoAlternoStyle = crearEstiloCuerpoAlterno(workbook);
            CellStyle numeroStyle = crearEstiloNumero(workbook);
            CellStyle monedaStyle = crearEstiloMoneda(workbook);
            CellStyle totalTextoStyle = crearEstiloTotalTexto(workbook);
            CellStyle totalNumeroStyle = crearEstiloTotalNumero(workbook);
            CellStyle totalMonedaStyle = crearEstiloTotalMoneda(workbook);

            String[] columnas = {
                "Método de Pago",
                "Cantidad de Transacciones",
                "Total Recaudado (S/)"
            };

            // Titulo principal
            Row filaTitulo = sheet.createRow(0);
            filaTitulo.setHeightInPoints(30);

            Cell titulo = filaTitulo.createCell(0);
            titulo.setCellValue("BODEGA LA PERUANITA");
            titulo.setCellStyle(tituloStyle);

            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

            // Subtitulo
            Row filaSubtitulo = sheet.createRow(1);
            Cell subtitulo = filaSubtitulo.createCell(0);
            subtitulo.setCellValue("REPORTE DE VENTAS POR MÉTODO DE PAGO");
            subtitulo.setCellStyle(subtituloStyle);

            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));

            // Periodo
            Row filaPeriodo = sheet.createRow(2);
            Cell celdaPeriodo = filaPeriodo.createCell(0);
            celdaPeriodo.setCellValue("Periodo: " + periodo);
            celdaPeriodo.setCellStyle(fechaStyle);

            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 2));

            // Fecha de generacion
            Row filaFecha = sheet.createRow(3);
            Cell celdaFecha = filaFecha.createCell(0);

            String fechaActual = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            );

            celdaFecha.setCellValue("Generado el: " + fechaActual);
            celdaFecha.setCellStyle(fechaStyle);

            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 2));

            // Encabezados
            Row headerRow = sheet.createRow(5);
            headerRow.setHeightInPoints(25);

            for (int i = 0; i < columnas.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Datos
            int rowNum = 6;

            for (Object[] fila : datos) {

                Row row = sheet.createRow(rowNum);

                Cell c1 = row.createCell(0);
                c1.setCellValue(String.valueOf(fila[0]));
                c1.setCellStyle(rowNum % 2 == 0
                        ? cuerpoAlternoStyle : cuerpoStyle);

                Cell c2 = row.createCell(1);
                c2.setCellValue(((Number) fila[1]).doubleValue());
                c2.setCellStyle(numeroStyle);

                Cell c3 = row.createCell(2);
                c3.setCellValue(((Number) fila[2]).doubleValue());
                c3.setCellStyle(monedaStyle);

                rowNum++;
            }

            // Fila de totales
            int ultimaFilaDatos = rowNum - 1;
            Row filaTotal = sheet.createRow(rowNum);

            Cell textoTotal = filaTotal.createCell(0);
            textoTotal.setCellValue("TOTAL GENERAL");
            textoTotal.setCellStyle(totalTextoStyle);

            Cell cantidadTotal = filaTotal.createCell(1);
            cantidadTotal.setCellFormula(
                "SUM(B7:B" + (ultimaFilaDatos + 1) + ")"
            );
            cantidadTotal.setCellStyle(totalNumeroStyle);

            Cell recaudacionTotal = filaTotal.createCell(2);
            recaudacionTotal.setCellFormula(
                "SUM(C7:C" + (ultimaFilaDatos + 1) + ")"
            );
            recaudacionTotal.setCellStyle(totalMonedaStyle);

            sheet.addMergedRegion(
                new CellRangeAddress(rowNum, rowNum, 0, 0)
            );

            // Configuracion de hoja
            configurarHoja(sheet, 3, 2, 5, rowNum);

            // Anchos de columnas
            sheet.setColumnWidth(0, 8000);
            sheet.setColumnWidth(1, 8500);
            sheet.setColumnWidth(2, 7500);

            workbook.setForceFormulaRecalculation(true);

            // Guardar
            try (FileOutputStream fileOut = new FileOutputStream(rutaArchivo)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (Exception e) {
            System.err.println(
                "Error al exportar métodos de pago: " + e.getMessage()
            );
            e.printStackTrace();
            return false;
        }
    }

    // ============================================================
    // CONFIGURACION GENERAL DE LAS HOJAS
    // ============================================================

    private static void configurarHoja(
            Sheet sheet, int columnas, int ultimaColumna,
            int filaEncabezado, int ultimaFila) {

        // Congelar encabezados
        sheet.createFreezePane(0, filaEncabezado + 1);

        // Filtros
        if (ultimaFila >= filaEncabezado + 1) {
            sheet.setAutoFilter(
                new CellRangeAddress(
                    filaEncabezado, ultimaFila - 1,
                    0, ultimaColumna
                )
            );
        }

        // Impresion horizontal
        sheet.getPrintSetup().setLandscape(true);
        sheet.getPrintSetup().setPaperSize(
            org.apache.poi.ss.usermodel.PrintSetup.A4_PAPERSIZE
        );

        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);

        sheet.setMargin(Sheet.LeftMargin, 0.35);
        sheet.setMargin(Sheet.RightMargin, 0.35);
        sheet.setMargin(Sheet.TopMargin, 0.50);
        sheet.setMargin(Sheet.BottomMargin, 0.50);

        sheet.setHorizontallyCenter(true);

        // Repetir encabezados al imprimir
        sheet.setRepeatingRows(
            new CellRangeAddress(filaEncabezado, filaEncabezado, 0, ultimaColumna)
        );
    }

    // ============================================================
    // ESTILOS
    // ============================================================

    private static CellStyle crearEstiloTitulo(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 18);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(
            org.apache.poi.ss.usermodel.IndexedColors.DARK_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private static CellStyle crearEstiloSubtitulo(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 13);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        return style;
    }

    private static CellStyle crearEstiloFecha(Workbook workbook) {

        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);

        return style;
    }

    private static CellStyle crearEstiloEncabezado(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(
            IndexedColors.DARK_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        aplicarBordes(style);

        return style;
    }

    private static CellStyle crearEstiloCuerpo(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        aplicarBordes(style);

        return style;
    }

    private static CellStyle crearEstiloCuerpoAlterno(Workbook workbook) {

        CellStyle style = crearEstiloCuerpo(workbook);

        style.setFillForegroundColor(
            IndexedColors.LIGHT_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private static CellStyle crearEstiloNumero(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(
            workbook.createDataFormat().getFormat("#,##0")
        );

        aplicarBordes(style);

        return style;
    }

    private static CellStyle crearEstiloMoneda(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        DataFormat format = workbook.createDataFormat();

        style.setDataFormat(
            format.getFormat("\"S/\" #,##0.00")
        );

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        aplicarBordes(style);

        return style;
    }

    private static CellStyle crearEstiloTotalTexto(Workbook workbook) {

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());

        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(
            IndexedColors.LIGHT_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);

        aplicarBordes(style);

        return style;
    }

    private static CellStyle crearEstiloTotalNumero(Workbook workbook) {

        CellStyle style = crearEstiloNumero(workbook);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(
            IndexedColors.LIGHT_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private static CellStyle crearEstiloTotalMoneda(Workbook workbook) {

        CellStyle style = crearEstiloMoneda(workbook);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_GREEN.getIndex());

        style.setFont(font);
        style.setFillForegroundColor(
            IndexedColors.LIGHT_GREEN.getIndex()
        );
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    // ============================================================
    // BORDES
    // ============================================================

    private static void aplicarBordes(CellStyle style) {

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
    }
}
