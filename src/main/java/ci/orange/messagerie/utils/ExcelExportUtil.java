package ci.orange.messagerie.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Classe utilitaire pour l'export Excel avec Apache POI
 * 
 * @author Geo
 */
public class ExcelExportUtil {

    /**
     * Exporte des données vers un fichier Excel
     * 
     * @param sheetName Le nom de la feuille Excel
     * @param headers Les en-têtes de colonnes
     * @param data Les données à exporter (chaque Map représente une ligne, les clés correspondent aux headers)
     * @return ByteArrayOutputStream contenant le fichier Excel
     * @throws IOException En cas d'erreur lors de la création du fichier
     */
    public static ByteArrayOutputStream exportToExcel(String sheetName, String[] headers, List<Map<String, Object>> data) throws IOException {
        // Créer un nouveau classeur Excel
        Workbook workbook = new XSSFWorkbook();
        
        // Créer un style pour les en-têtes
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // Créer un style pour les cellules de données
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        // Créer la feuille
        Sheet sheet = workbook.createSheet(sheetName);
        
        // Créer la ligne d'en-têtes
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Remplir les données
        int rowNum = 1;
        for (Map<String, Object> rowData : data) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.createCell(i);
                Object value = rowData.get(headers[i]);
                setCellValue(cell, value);
                cell.setCellStyle(dataStyle);
            }
        }
        
        // Ajuster la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Ajouter un peu de padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
        
        // Écrire le classeur dans un ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream;
    }

    /**
     * Définit la valeur d'une cellule selon le type de données
     * 
     * @param cell La cellule à remplir
     * @param value La valeur à insérer
     */
    private static void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            if (value instanceof Integer || value instanceof Long) {
                cell.setCellValue(((Number) value).doubleValue());
            } else {
                cell.setCellValue(((Number) value).doubleValue());
            }
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
            // Optionnel : appliquer un format de date
            CellStyle dateStyle = cell.getSheet().getWorkbook().createCellStyle();
            DataFormat format = cell.getSheet().getWorkbook().createDataFormat();
            dateStyle.setDataFormat(format.getFormat("dd/mm/yyyy HH:mm"));
            cell.setCellStyle(dateStyle);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    /**
     * Exporte des données vers un fichier Excel (version simplifiée avec une liste de listes)
     * 
     * @param sheetName Le nom de la feuille Excel
     * @param headers Les en-têtes de colonnes
     * @param rows Les données (chaque liste représente une ligne)
     * @return ByteArrayOutputStream contenant le fichier Excel
     * @throws IOException En cas d'erreur lors de la création du fichier
     */
    public static ByteArrayOutputStream exportToExcelSimple(String sheetName, String[] headers, List<List<Object>> rows) throws IOException {
        // Créer un nouveau classeur Excel
        Workbook workbook = new XSSFWorkbook();
        
        // Créer un style pour les en-têtes
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        
        // Créer un style pour les cellules de données
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);
        
        // Créer la feuille
        Sheet sheet = workbook.createSheet(sheetName);
        
        // Créer la ligne d'en-têtes
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // Remplir les données
        int rowNum = 1;
        for (List<Object> rowData : rows) {
            Row row = sheet.createRow(rowNum++);
            for (int i = 0; i < Math.min(rowData.size(), headers.length); i++) {
                Cell cell = row.createCell(i);
                Object value = rowData.get(i);
                setCellValue(cell, value);
                cell.setCellStyle(dataStyle);
            }
        }
        
        // Ajuster la largeur des colonnes
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            // Ajouter un peu de padding
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1000);
        }
        
        // Écrire le classeur dans un ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream;
    }
}
