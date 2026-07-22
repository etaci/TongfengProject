package com.tongfeng.backend.app;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class DoctorVisitPdfRenderer {

	private static final int IMAGE_WIDTH = 1240;
	private static final int IMAGE_HEIGHT = 1754;
	private static final int PAGE_WIDTH = 595;
	private static final int PAGE_HEIGHT = 842;

	public byte[] render(AppContracts.DoctorVisitPackageResponse data) {
		List<String> lines = buildLines(data);
		List<byte[]> pages = renderPages(lines);
		return writePdf(pages);
	}

	private List<String> buildLines(AppContracts.DoctorVisitPackageResponse data) {
		List<String> lines = new ArrayList<>();
		lines.add("痛风/高尿酸医生就诊包");
		lines.add("患者：" + data.patientName() + "    统计周期：近 " + data.lookbackDays() + " 天");
		lines.add("目标尿酸：" + (data.targetUricAcid() == null ? "未设置" : data.targetUricAcid() + " umol/L"));
		lines.add("");
		lines.add("尿酸趋势");
		data.uricAcidTrend().forEach(item -> lines.add("- " + item.date() + "  " + item.value() + " " + item.unit()));
		lines.add("");
		lines.add("发作记录");
		data.flareRecords().forEach(item -> lines.add("- " + item.startedAt() + "  " + item.joint() + "  疼痛 " + item.painLevel()));
		lines.add("");
		lines.add("用药依从");
		lines.add("- 计划 " + data.medicationAdherence().plannedDoseCount() + " 次，已服 "
				+ data.medicationAdherence().takenDoseCount() + " 次，依从率 " + data.medicationAdherence().adherenceRate() + "%");
		lines.add("");
		lines.add("化验单可信状态");
		data.labReports().forEach(item -> lines.add("- " + item.reportDate() + "  " + item.riskLevel()
				+ "  " + item.verificationStatus() + "  来源：" + item.sourceType()));
		if (data.latestTriage() != null) {
			lines.add("");
			lines.add("最近痛风分诊");
			lines.add("- " + data.latestTriage().triageCode() + " / " + data.latestTriage().decisionCode());
			lines.add("- 规则：" + data.latestTriage().ruleVersion() + " / " + data.latestTriage().verificationStatus());
			lines.addAll(data.latestTriage().redFlags().stream().map(item -> "- 红旗：" + item).toList());
		}
		lines.add("");
		lines.add("待问医生的问题");
		lines.addAll(data.questionsForDoctor().stream().map(item -> "- " + item).toList());
		lines.add("");
		lines.add("数据来源与可信边界");
		lines.addAll(data.trustNotes().stream().map(item -> "- " + item).toList());
		lines.add("生成时间：" + data.generatedAt());
		return lines;
	}

	private List<byte[]> renderPages(List<String> sourceLines) {
		List<byte[]> pages = new ArrayList<>();
		BufferedImage probe = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		Graphics2D probeGraphics = probe.createGraphics();
		Font font = new Font("SansSerif", Font.PLAIN, 28);
		probeGraphics.setFont(font);
		List<String> wrapped = new ArrayList<>();
		for (String line : sourceLines) {
			wrapped.addAll(wrap(line, probeGraphics.getFontMetrics(), 1080));
		}
		probeGraphics.dispose();
		int linesPerPage = 45;
		for (int offset = 0; offset < wrapped.size(); offset += linesPerPage) {
			List<String> pageLines = wrapped.subList(offset, Math.min(offset + linesPerPage, wrapped.size()));
			pages.add(renderPage(pageLines, font, offset == 0));
		}
		if (pages.isEmpty()) {
			pages.add(renderPage(List.of("医生就诊包暂无数据"), font, true));
		}
		return pages;
	}

	private byte[] renderPage(List<String> lines, Font font, boolean firstPage) {
		BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.WHITE);
		graphics.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
		graphics.setColor(new Color(32, 39, 45));
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setFont(font);
		int y = 85;
		for (int i = 0; i < lines.size(); i++) {
			if (firstPage && i == 0) {
				graphics.setFont(font.deriveFont(Font.BOLD, 42f));
				graphics.drawString(lines.get(i), 70, y);
				graphics.setFont(font);
				y += 60;
			} else {
				graphics.drawString(lines.get(i), 70, y);
				y += 35;
			}
		}
		graphics.dispose();
		try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			ImageIO.write(image, "jpg", output);
			return output.toByteArray();
		} catch (IOException ex) {
			throw new BusinessException("DOCTOR_PACKAGE_PDF_FAILED", "生成就诊包 PDF 页面失败");
		}
	}

	private List<String> wrap(String line, FontMetrics metrics, int maxWidth) {
		if (line == null || line.isEmpty()) {
			return List.of("");
		}
		List<String> result = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int i = 0; i < line.length(); i++) {
			char value = line.charAt(i);
			if (metrics.stringWidth(current.toString() + value) > maxWidth && !current.isEmpty()) {
				result.add(current.toString());
				current.setLength(0);
			}
			current.append(value);
		}
		result.add(current.toString());
		return result;
	}

	private byte[] writePdf(List<byte[]> images) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			output.write("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n".getBytes(StandardCharsets.ISO_8859_1));
			int objectCount = 2 + images.size() * 3;
			long[] offsets = new long[objectCount + 1];
			String kids = java.util.stream.IntStream.range(0, images.size())
					.mapToObj(index -> (3 + index * 3) + " 0 R")
					.collect(java.util.stream.Collectors.joining(" "));
			writeObject(output, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");
			writeObject(output, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + images.size() + " >>");
			for (int i = 0; i < images.size(); i++) {
				int pageId = 3 + i * 3;
				int contentId = pageId + 1;
				int imageId = pageId + 2;
				writeObject(output, offsets, pageId, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + PAGE_WIDTH + " " + PAGE_HEIGHT
						+ "] /Resources << /XObject << /Im0 " + imageId + " 0 R >> >> /Contents " + contentId + " 0 R >>");
				byte[] content = ("q " + PAGE_WIDTH + " 0 0 " + PAGE_HEIGHT + " 0 0 cm /Im0 Do Q").getBytes(StandardCharsets.US_ASCII);
				writeStreamObject(output, offsets, contentId, "<< /Length " + content.length + " >>", content);
				byte[] image = images.get(i);
				writeStreamObject(output, offsets, imageId, "<< /Type /XObject /Subtype /Image /Width " + IMAGE_WIDTH
						+ " /Height " + IMAGE_HEIGHT + " /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length "
						+ image.length + " >>", image);
			}
			long xrefOffset = output.size();
			output.write(("xref\n0 " + (objectCount + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
			output.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
			for (int i = 1; i <= objectCount; i++) {
				output.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.US_ASCII));
			}
			output.write(("trailer\n<< /Size " + (objectCount + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF")
					.getBytes(StandardCharsets.US_ASCII));
			return output.toByteArray();
		} catch (IOException ex) {
			throw new BusinessException("DOCTOR_PACKAGE_PDF_FAILED", "生成就诊包 PDF 失败");
		}
	}

	private void writeObject(ByteArrayOutputStream output, long[] offsets, int id, String body) throws IOException {
		offsets[id] = output.size();
		output.write((id + " 0 obj\n" + body + "\nendobj\n").getBytes(StandardCharsets.US_ASCII));
	}

	private void writeStreamObject(ByteArrayOutputStream output, long[] offsets, int id, String dictionary, byte[] stream) throws IOException {
		offsets[id] = output.size();
		output.write((id + " 0 obj\n" + dictionary + "\nstream\n").getBytes(StandardCharsets.US_ASCII));
		output.write(stream);
		output.write("\nendstream\nendobj\n".getBytes(StandardCharsets.US_ASCII));
	}
}
