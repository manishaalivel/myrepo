import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import java.io.StringReader;

public class App {
    private static final Color INK = new Color(28, 39, 55);
    private static final Color MUTED = new Color(101, 114, 132);
    private static final Color ACCENT = new Color(37, 99, 235);
    private static final Set<String> STOP_WORDS = new LinkedHashSet<>(Arrays.asList(
        "about", "above", "after", "again", "against", "also", "and", "any", "are", "because",
        "been", "before", "being", "below", "between", "both", "but", "can", "could", "data",
        "each", "for", "from", "further", "have", "having", "into", "its", "more", "most",
        "must", "not", "our", "out", "over", "same", "should", "such", "than", "that", "the",
        "their", "then", "there", "these", "they", "this", "those", "through", "under", "use",
        "used", "using", "very", "was", "were", "what", "when", "where", "which", "while",
        "with", "within", "will", "would", "you", "your", "years", "year", "ability", "work",
        "team", "including", "required", "preferred", "candidate", "role", "responsibilities"
    ));

    private final JFrame frame = new JFrame("ResumeFit | ATS Resume Editor");
    private final JTextArea jobDescription = new JTextArea();
    private final JTextArea resumeDraft = new JTextArea();
    private final JLabel fileStatus = new JLabel("No resume uploaded");
    private final JLabel matchStatus = new JLabel("Upload a resume and add a job description to get started.");
    private final JComboBox<String> formatPicker = new JComboBox<>(
        new String[] { "PDF", "Word document (.docx)", "Plain text (.txt)", "Rich text (.rtf)", "HTML", "Markdown" }
    );
    private Path uploadedResume;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().show());
    }

    private void show() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            System.err.println("Could not load the system look and feel: " + ex.getMessage());
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(960, 680));
        frame.setSize(1180, 800);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(18, 16));
        root.setBorder(BorderFactory.createEmptyBorder(22, 24, 20, 24));
        root.setBackground(new Color(246, 248, 252));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildWorkspace(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(8, 4));
        header.setOpaque(false);
        JLabel title = new JLabel("Make your resume fit the role");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Create a clean, ATS-friendly draft while keeping your experience yours.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(MUTED);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 3));
        text.setOpaque(false);
        text.add(title);
        text.add(subtitle);
        header.add(text, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildWorkspace() {
        JPanel workspace = new JPanel(new GridLayout(1, 2, 18, 0));
        workspace.setOpaque(false);
        workspace.add(buildInputs());
        workspace.add(buildEditor());
        return workspace;
    }

    private JPanel buildInputs() {
        JPanel panel = card();
        panel.setLayout(new BorderLayout(0, 14));

        JPanel uploadArea = new JPanel(new BorderLayout(8, 8));
        uploadArea.setOpaque(false);
        JLabel resumeLabel = sectionLabel("1  YOUR RESUME");
        JButton uploadButton = new JButton("Upload resume");
        uploadButton.addActionListener(event -> chooseResume());
        JPanel uploadHeader = new JPanel(new BorderLayout(8, 0));
        uploadHeader.setOpaque(false);
        uploadHeader.add(resumeLabel, BorderLayout.WEST);
        uploadHeader.add(uploadButton, BorderLayout.EAST);
        fileStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        fileStatus.setForeground(MUTED);
        uploadArea.add(uploadHeader, BorderLayout.NORTH);
        uploadArea.add(fileStatus, BorderLayout.CENTER);

        JPanel jobArea = new JPanel(new BorderLayout(0, 8));
        jobArea.setOpaque(false);
        jobArea.add(sectionLabel("2  JOB DESCRIPTION"), BorderLayout.NORTH);
        jobDescription.setFont(new Font("SansSerif", Font.PLAIN, 13));
        jobDescription.setLineWrap(true);
        jobDescription.setWrapStyleWord(true);
        jobDescription.setText("");
        JScrollPane jobScroll = new JScrollPane(jobDescription);
        jobScroll.setBorder(BorderFactory.createLineBorder(new Color(221, 227, 236)));
        jobArea.add(jobScroll, BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setOpaque(false);
        content.add(uploadArea, BorderLayout.NORTH);
        content.add(jobArea, BorderLayout.CENTER);
        panel.add(content, BorderLayout.CENTER);

        JButton optimizeButton = new JButton("Build ATS-friendly draft");
        optimizeButton.setForeground(Color.WHITE);
        optimizeButton.setBackground(ACCENT);
        optimizeButton.setOpaque(true);
        optimizeButton.setBorderPainted(false);
        optimizeButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        optimizeButton.addActionListener(event -> optimizeResume());
        panel.add(optimizeButton, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildEditor() {
        JPanel panel = card();
        panel.setLayout(new BorderLayout(0, 12));
        JPanel heading = new JPanel(new BorderLayout(0, 5));
        heading.setOpaque(false);
        heading.add(sectionLabel("3  REVIEW & EDIT"), BorderLayout.NORTH);
        matchStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        matchStatus.setForeground(MUTED);
        heading.add(matchStatus, BorderLayout.CENTER);
        panel.add(heading, BorderLayout.NORTH);

        resumeDraft.setFont(new Font("Monospaced", Font.PLAIN, 13));
        resumeDraft.setLineWrap(true);
        resumeDraft.setWrapStyleWord(true);
        resumeDraft.setText("");
        JScrollPane draftScroll = new JScrollPane(resumeDraft);
        draftScroll.setBorder(BorderFactory.createLineBorder(new Color(221, 227, 236)));
        panel.add(draftScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JLabel note = new JLabel("Your resume stays on this computer. Review every edit before applying.");
        note.setForeground(MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 12));
        footer.add(note, BorderLayout.CENTER);
        JPanel controls = new JPanel(new BorderLayout(8, 0));
        controls.setOpaque(false);
        formatPicker.setFont(new Font("SansSerif", Font.PLAIN, 12));
        controls.add(formatPicker, BorderLayout.CENTER);
        JButton downloadButton = new JButton("Download");
        downloadButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        downloadButton.addActionListener(event -> downloadResume());
        controls.add(downloadButton, BorderLayout.EAST);
        footer.add(controls, BorderLayout.EAST);
        return footer;
    }

    private JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 233, 241)),
            BorderFactory.createEmptyBorder(17, 17, 17, 17)
        ));
        return panel;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(INK);
        return label;
    }

    private void chooseResume() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose a resume");
        chooser.setFileFilter(new FileNameExtensionFilter(
            "Supported resumes (.txt, .md, .html, .htm, .rtf, .docx)",
            "txt", "md", "html", "htm", "rtf", "docx"
        ));
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        try {
            String text = readResume(selected);
            if (text.isBlank()) {
                throw new IOException("The selected file did not contain readable resume text.");
            }
            uploadedResume = selected;
            resumeDraft.setText(formatForAts(text));
            fileStatus.setText(selected.getFileName().toString());
            matchStatus.setText("Resume loaded. Add the job description, then build your draft.");
        } catch (IOException ex) {
            showError("Could not open resume", ex);
        }
    }

    private void optimizeResume() {
        if (uploadedResume == null || resumeDraft.getText().isBlank()) {
            JOptionPane.showMessageDialog(frame, "Upload a resume before building a draft.", "Resume needed",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String description = jobDescription.getText().trim();
        if (description.isBlank()) {
            JOptionPane.showMessageDialog(frame, "Paste the job description to check keyword alignment.",
                "Job description needed", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        resumeDraft.setText(prioritizeMatchingSkills(formatForAts(resumeDraft.getText()), description));
        List<String> terms = extractKeywords(description);
        List<String> matched = new ArrayList<>();
        List<String> absent = new ArrayList<>();
        String resume = resumeDraft.getText().toLowerCase(Locale.ROOT);
        for (String term : terms) {
            if (containsTerm(resume, term)) {
                matched.add(term);
            } else {
                absent.add(term);
            }
        }
        int score = terms.isEmpty() ? 0 : (int) Math.round(100.0 * matched.size() / terms.size());
        StringBuilder status = new StringBuilder("Keyword overlap: ").append(score).append("%  |  ")
            .append(matched.size()).append(" of ").append(terms.size()).append(" job-description terms found.");
        if (!matched.isEmpty()) {
            status.append("\nFound: ").append(joinLimited(matched, 10));
        }
        if (!absent.isEmpty()) {
            status.append("\nNot found in your resume: ").append(joinLimited(absent, 10))
                .append(" — add only if accurate.");
        }
        matchStatus.setText("<html>" + escapeHtml(status.toString()).replace("\n", "<br>") + "</html>");
    }

    private void downloadResume() {
        String content = resumeDraft.getText().trim();
        if (content.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Upload or enter a resume before downloading.", "Resume needed",
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String selected = (String) formatPicker.getSelectedItem();
        String extension = selected.startsWith("Word") ? "docx"
            : selected.startsWith("Plain") ? "txt"
            : selected.startsWith("Rich") ? "rtf"
            : selected.equals("Markdown") ? "md"
            : selected.equals("HTML") ? "html" : "pdf";
        String baseName = uploadedResume == null ? "ats-resume" : removeExtension(uploadedResume.getFileName().toString());
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Download your resume");
        chooser.setSelectedFile(Path.of(baseName + "-ats." + extension).toFile());
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path destination = chooser.getSelectedFile().toPath();
        if (!destination.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + extension)) {
            destination = destination.resolveSibling(destination.getFileName() + "." + extension);
        }
        try {
            writeResume(destination, extension, content);
            JOptionPane.showMessageDialog(frame, "Resume saved to:\n" + destination.toAbsolutePath(),
                "Download complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Could not save resume", ex);
        }
    }

    private String readResume(Path file) throws IOException {
        String extension = extension(file);
        if (extension.equals("docx")) {
            return readDocx(file);
        }
        if (extension.equals("pdf")) {
            throw new IOException("PDF upload is not supported yet. Upload a .docx, .txt, .md, .html, or .rtf file.");
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        if (extension.equals("html") || extension.equals("htm")) {
            return htmlToText(text);
        }
        if (extension.equals("rtf")) {
            return rtfToText(text);
        }
        if (!Arrays.asList("txt", "md").contains(extension)) {
            throw new IOException("Unsupported file type. Upload .docx, .txt, .md, .html, or .rtf.");
        }
        return text;
    }

    private String readDocx(Path file) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("word/document.xml".equals(entry.getName())) {
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    zip.transferTo(buffer);
                    DocumentBuilderFactory factory = secureXmlFactory();
                    Document document = factory.newDocumentBuilder().parse(
                        new InputSource(new StringReader(buffer.toString(StandardCharsets.UTF_8)))
                    );
                    NodeList paragraphs = document.getElementsByTagNameNS("*", "p");
                    List<String> lines = new ArrayList<>();
                    for (int i = 0; i < paragraphs.getLength(); i++) {
                        StringBuilder line = new StringBuilder();
                        Node paragraph = paragraphs.item(i);
                        if (((org.w3c.dom.Element) paragraph).getElementsByTagNameNS("*", "numPr").getLength() > 0) {
                            line.append("- ");
                        }
                        appendWordText(paragraph, line);
                        if (!line.isEmpty()) {
                            lines.add(line.toString());
                        } else if (paragraph.hasChildNodes()) {
                            lines.add("");
                        }
                    }
                    return String.join("\n", lines);
                }
            }
            throw new IOException("This Word document does not contain a readable document body.");
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Could not read the Word document: " + ex.getMessage(), ex);
        }
    }

    private static void appendWordText(Node node, StringBuilder output) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if ("t".equals(node.getLocalName())) {
                output.append(node.getTextContent());
                return;
            }
            if ("tab".equals(node.getLocalName())) {
                output.append('\t');
                return;
            }
            if ("br".equals(node.getLocalName())) {
                output.append('\n');
                return;
            }
        }
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            appendWordText(children.item(i), output);
        }
    }

    private static DocumentBuilderFactory secureXmlFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private static String htmlToText(String html) {
        String text = html.replaceAll("(?is)<(script|style)[^>]*>.*?</\\1>", " ");
        text = text.replaceAll("(?i)<br\\s*/?>|</(p|div|li|h[1-6]|tr)>", "\n");
        text = text.replaceAll("(?s)<[^>]*>", " ");
        text = text.replace("&nbsp;", " ").replace("&#160;", " ")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'");
        return text.replaceAll("[ \\t]+", " ").replaceAll(" *\\n *", "\n");
    }

    private static String rtfToText(String rtf) {
        String text = rtf.replaceAll("(?s)\\\\\\*\\\\[^ ]+ ?", "")
            .replaceAll("\\\\'[0-9a-fA-F]{2}", " ")
            .replaceAll("\\\\(par|line)\\b ?", "\n")
            .replaceAll("\\\\tab\\b ?", "\t")
            .replaceAll("\\\\[a-zA-Z]+-?\\d* ?", "")
            .replaceAll("\\\\[^a-zA-Z]", "")
            .replace("{", "").replace("}", "");
        return text;
    }

    private static String formatForAts(String source) {
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n')
            .replace('\u00a0', ' ').replace('\u2022', '-').replace('\u25aa', '-');
        StringBuilder output = new StringBuilder();
        int blankLines = 0;
        for (String original : normalized.split("\n", -1)) {
            String line = original.trim();
            if (line.startsWith("#")) {
                line = line.replaceFirst("^#{1,6}\\s*", "");
            }
            line = line.replaceAll("\\*\\*(.*?)\\*\\*", "$1").replaceAll("__(.*?)__", "$1");
            String heading = canonicalHeading(line);
            if (heading != null) {
                line = heading.toUpperCase(Locale.ROOT);
            } else if (line.matches("^[*+•▪◦]\\s+.*")) {
                line = "- " + line.replaceFirst("^[*+•▪◦]\\s+", "");
            } else if (line.matches("^\\d+[.)]\\s+.*")) {
                line = line.replaceFirst("^\\d+[.)]\\s+", "- ");
            }
            if (line.isBlank()) {
                if (blankLines == 0 && output.length() > 0) {
                    output.append('\n');
                }
                blankLines++;
            } else {
                if (output.length() > 0 && output.charAt(output.length() - 1) != '\n') {
                    output.append('\n');
                }
                output.append(line).append('\n');
                blankLines = 0;
            }
        }
        return output.toString().trim();
    }

    private static String prioritizeMatchingSkills(String source, String jobDescription) {
        List<String> terms = extractKeywords(jobDescription);
        if (terms.isEmpty()) {
            return source;
        }
        List<String> lines = new ArrayList<>(Arrays.asList(source.split("\n", -1)));
        int skillsHeading = -1;
        int sectionEnd = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            String heading = canonicalHeading(lines.get(i));
            if (heading == null) {
                continue;
            }
            if (heading.equals("Skills")) {
                skillsHeading = i;
            } else if (skillsHeading >= 0) {
                sectionEnd = i;
                break;
            }
        }
        if (skillsHeading < 0 || skillsHeading + 1 >= sectionEnd) {
            return source;
        }

        List<String> entries = new ArrayList<>();
        for (int i = skillsHeading + 1; i < sectionEnd; i++) {
            String line = lines.get(i).trim();
            if (!line.isEmpty()) {
                entries.add(line);
            }
        }
        String separator = null;
        if (entries.size() == 1 && (entries.get(0).contains(",") || entries.get(0).contains("|"))) {
            separator = entries.get(0).contains("|") ? " | " : ", ";
            entries = new ArrayList<>(Arrays.asList(entries.get(0).split("\\s*[,|]\\s*")));
        }
        entries.sort((first, second) ->
            Integer.compare(skillMatchCount(second, terms), skillMatchCount(first, terms)));

        List<String> prioritized = new ArrayList<>(lines.subList(0, skillsHeading + 1));
        if (separator == null) {
            prioritized.addAll(entries);
        } else {
            prioritized.add(String.join(separator, entries));
        }
        prioritized.addAll(lines.subList(sectionEnd, lines.size()));
        return String.join("\n", prioritized);
    }

    private static int skillMatchCount(String skill, List<String> terms) {
        int count = 0;
        for (String term : terms) {
            if (containsTerm(skill.toLowerCase(Locale.ROOT), term)) {
                count++;
            }
        }
        return count;
    }

    private static String canonicalHeading(String line) {
        String key = line.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", "").trim();
        Map<String, String> headings = new LinkedHashMap<>();
        headings.put("summary", "Professional Summary");
        headings.put("professional summary", "Professional Summary");
        headings.put("career summary", "Professional Summary");
        headings.put("objective", "Professional Summary");
        headings.put("profile", "Professional Summary");
        headings.put("experience", "Professional Experience");
        headings.put("work experience", "Professional Experience");
        headings.put("professional experience", "Professional Experience");
        headings.put("employment history", "Professional Experience");
        headings.put("work history", "Professional Experience");
        headings.put("skills", "Skills");
        headings.put("technical skills", "Skills");
        headings.put("core competencies", "Skills");
        headings.put("education", "Education");
        headings.put("academic background", "Education");
        headings.put("certifications", "Certifications");
        headings.put("licenses and certifications", "Certifications");
        headings.put("projects", "Projects");
        headings.put("selected projects", "Projects");
        headings.put("publications", "Publications");
        headings.put("volunteer experience", "Volunteer Experience");
        return headings.get(key);
    }

    private static List<String> extractKeywords(String text) {
        Map<String, Integer> frequencies = new LinkedHashMap<>();
        Matcher matcher = Pattern.compile("[a-zA-Z][a-zA-Z0-9+#.\\-]{2,}").matcher(text.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String word = matcher.group().replaceAll("^[.\\-]+|[.\\-]+$", "");
            if (word.length() > 2 && !STOP_WORDS.contains(word) && !word.matches("\\d+")) {
                frequencies.put(word, frequencies.getOrDefault(word, 0) + 1);
            }
        }
        List<String> terms = new ArrayList<>(frequencies.keySet());
        terms.sort((first, second) -> Integer.compare(frequencies.get(second), frequencies.get(first)));
        return terms.subList(0, Math.min(12, terms.size()));
    }

    private static boolean containsTerm(String text, String term) {
        return Pattern.compile("(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])")
            .matcher(text).find();
    }

    private static String joinLimited(List<String> terms, int limit) {
        return String.join(", ", terms.subList(0, Math.min(limit, terms.size())));
    }

    private void writeResume(Path destination, String extension, String content) throws IOException {
        switch (extension) {
            case "docx":
                writeDocx(destination, content);
                break;
            case "pdf":
                Files.write(destination, createPdf(content));
                break;
            case "html":
                Files.writeString(destination, toHtml(content), StandardCharsets.UTF_8);
                break;
            case "rtf":
                Files.writeString(destination, toRtf(content), StandardCharsets.UTF_8);
                break;
            default:
                Files.writeString(destination, content + System.lineSeparator(), StandardCharsets.UTF_8);
        }
    }

    private static void writeDocx(Path destination, String content) throws IOException {
        StringBuilder paragraphs = new StringBuilder();
        for (String line : content.split("\\R", -1)) {
            paragraphs.append("<w:p><w:r><w:t xml:space=\"preserve\">")
                .append(escapeXml(line)).append("</w:t></w:r></w:p>");
        }
        String document = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
            + "<w:body>" + paragraphs
            + "<w:sectPr><w:pgSz w:w=\"12240\" w:h=\"15840\"/>"
            + "<w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/>"
            + "</w:sectPr></w:body></w:document>";
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destination))) {
            putZipEntry(zip, "[Content_Types].xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" "
                + "ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>");
            putZipEntry(zip, "_rels/.rels",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>");
            putZipEntry(zip, "word/document.xml", document);
        }
    }

    private static void putZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static byte[] createPdf(String content) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String sourceLine : content.split("\\R", -1)) {
            String line = toPdfText(sourceLine);
            while (line.length() > 92) {
                int split = line.lastIndexOf(' ', 92);
                if (split < 1) {
                    split = 92;
                }
                lines.add(line.substring(0, split));
                line = line.substring(Math.min(split + 1, line.length()));
            }
            lines.add(line);
        }
        List<List<String>> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += 52) {
            pages.add(lines.subList(start, Math.min(start + 52, lines.size())));
        }
        if (pages.isEmpty()) {
            pages.add(List.of(""));
        }
        int pageCount = pages.size();
        List<String> objects = new ArrayList<>();
        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(4 + i * 2).append(" 0 R ");
        }
        objects.add("<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        for (int i = 0; i < pageCount; i++) {
            int pageObject = 4 + i * 2;
            int streamObject = pageObject + 1;
            StringBuilder stream = new StringBuilder("BT\n/F1 10 Tf\n50 790 Td\n12 TL\n");
            for (String line : pages.get(i)) {
                stream.append('(').append(escapePdf(line)).append(") Tj\nT*\n");
            }
            stream.append("ET");
            objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
                + "/Resources << /Font << /F1 3 0 R >> >> /Contents " + streamObject + " 0 R >>");
            objects.add("<< /Length " + stream.toString().getBytes(StandardCharsets.ISO_8859_1).length
                + " >>\nstream\n" + stream + "\nendstream");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(output.size());
            output.write(((i + 1) + " 0 obj\n" + objects.get(i) + "\nendobj\n")
                .getBytes(StandardCharsets.ISO_8859_1));
        }
        int xref = output.size();
        output.write(("xref\n0 " + (objects.size() + 1) + "\n0000000000 65535 f \n")
            .getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i < offsets.size(); i++) {
            output.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i))
                .getBytes(StandardCharsets.ISO_8859_1));
        }
        output.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
            + xref + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
        return output.toByteArray();
    }

    private static String toPdfText(String text) {
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            safe.append(character >= 32 && character <= 126 ? character : '?');
        }
        return safe.toString();
    }

    private static String escapePdf(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String toHtml(String text) {
        StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"utf-8\">"
            + "<title>Resume</title><style>body{font-family:Arial,sans-serif;max-width:760px;margin:40px auto;"
            + "line-height:1.45;color:#222}pre{white-space:pre-wrap;font-family:Arial,sans-serif}</style>"
            + "</head><body><pre>");
        html.append(escapeHtml(text)).append("</pre></body></html>");
        return html.toString();
    }

    private static String toRtf(String text) {
        StringBuilder rtf = new StringBuilder("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\f0\\fs22 ");
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character == '\\' || character == '{' || character == '}') {
                rtf.append('\\').append(character);
            } else if (character == '\n') {
                rtf.append("\\par\n");
            } else if (character == '\r') {
                continue;
            } else if (character > 127) {
                rtf.append("\\u").append((short) character).append('?');
            } else {
                rtf.append(character);
            }
        }
        return rtf.append('}').toString();
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeXml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String removeExtension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(0, dot);
    }

    private void showError(String title, Exception exception) {
        JOptionPane.showMessageDialog(frame, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    }
}
