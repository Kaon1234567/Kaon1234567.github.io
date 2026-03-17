import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.Timer;

public class QuizGame extends JFrame {
    private final JLabel characterLabel, questionLabel, resultLabel, titleLabel;
    private final JTextField answerField;
    private final JButton submitButton, dictButton, hintButton, startButton, resetButton;
    private final List<String[]> masterList = new ArrayList<>();
    private final List<String[]> sessionQuizzes = new ArrayList<>();
    
    // ★ 正解済み用語を記録するセット
    private final Set<String> clearedTerms = new HashSet<>(); 
    
    private int currentIndex = 0, missCount = 0, hintUsed = 0, giveUpCount = 0;
    private int currentHintStep = 0; 
    private long startTime;
    private String currentAnswer = "";
    private boolean isAnswering = true;
    private Clip bgmClip;

    public QuizGame() {
        setTitle("用語変換");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // ★ 画面最大化とEscキー対応
        setExtendedState(JFrame.MAXIMIZED_BOTH); 
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
            KeyStroke.getKeyStroke("ESCAPE"), "escapeAction");
        getRootPane().getActionMap().put("escapeAction", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                int res = JOptionPane.showConfirmDialog(QuizGame.this, "ゲームを終了しますか？", "終了確認", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) System.exit(0);
            }
        });

        setLayout(null);
        getContentPane().setBackground(new Color(20, 20, 25));

        loadCSV("quiz_data3.csv");
        loadClearedTerms(); // ★ 保存された正解記録を読み込む
        playBGM("bgm.wav");

        characterLabel = new JLabel();
        titleLabel = new JLabel("用語変換", SwingConstants.CENTER);
        titleLabel.setFont(new Font("MS Gothic", Font.BOLD, 72));
        titleLabel.setForeground(new Color(255, 215, 0));
        
        startButton = new JButton("START");
        startButton.setFont(new Font("Arial", Font.BOLD, 30));
        startButton.addActionListener(e -> startGame());
        
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setFont(new Font("MS Gothic", Font.BOLD, 32));
        questionLabel.setForeground(Color.WHITE);

        answerField = new JTextField();
        answerField.setFont(new Font("MS Gothic", Font.PLAIN, 24));
        answerField.addActionListener(e -> checkAnswer());

        submitButton = new JButton("送信");
        submitButton.addActionListener(e -> checkAnswer());

        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setFont(new Font("MS Gothic", Font.BOLD, 40));

        dictButton = new JButton("図鑑");
        dictButton.addActionListener(e -> openDictionary());

        hintButton = new JButton("ヒント");
        hintButton.addActionListener(e -> showCurrentHint());

        resetButton = new JButton("リセット");
        resetButton.addActionListener(e -> initStartScreen());

        add(titleLabel); add(startButton); add(questionLabel);
        add(answerField); add(submitButton); add(resultLabel);
        add(dictButton); add(hintButton); add(resetButton); add(characterLabel);

        // ★ 画面リサイズ時に位置を再計算
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                relayout();
            }
        });

        initStartScreen();
        setVisible(true);
    }

    private void relayout() {
        int w = getWidth();
        int h = getHeight();
        characterLabel.setBounds(w - 500, h - 700, 450, 650);
        titleLabel.setBounds((w - 600) / 2 - 200, h / 2 - 150, 550, 100);
        startButton.setBounds((w - 600) / 2 - 25, h / 2 + 50, 200, 80);
        questionLabel.setBounds(50, 80, w - 600, 300);
        answerField.setBounds(w / 2 - 250, h / 2 + 50, 250, 50);
        submitButton.setBounds(w / 2 + 10, h / 2 + 50, 100, 50);
        resultLabel.setBounds(50, h / 2 - 50, w - 600, 80);
        dictButton.setBounds(20, h - 100, 100, 40);
        hintButton.setBounds(130, h - 100, 100, 40);
        resetButton.setBounds(240, h - 100, 100, 40);
        revalidate(); repaint();
    }

    private void initStartScreen() { 
        getContentPane().setBackground(new Color(20, 20, 25));
        showUI(true); 
        updateCharacter("スタート"); 
    }

    private void showUI(boolean isStart) {
        titleLabel.setVisible(isStart); startButton.setVisible(isStart);
        questionLabel.setVisible(!isStart); answerField.setVisible(!isStart);
        submitButton.setVisible(!isStart); resultLabel.setVisible(!isStart);
        dictButton.setVisible(!isStart); hintButton.setVisible(!isStart);
        resetButton.setVisible(!isStart);
    }

    private void startGame() {
        showUI(false);
        Collections.shuffle(masterList);
        sessionQuizzes.clear();
        for(int i=0; i<5 && i<masterList.size(); i++) sessionQuizzes.add(masterList.get(i));
        currentIndex = 0; missCount = 0; hintUsed = 0; giveUpCount = 0;
        startTime = System.currentTimeMillis();
        showNextQuestion();
    }

    private void showNextQuestion() {
        currentHintStep = 0; 
        if (currentIndex < sessionQuizzes.size()) {
            isAnswering = true;
            String[] data = sessionQuizzes.get(currentIndex);
            questionLabel.setText("<html><center>" + data[0] + "</center></html>"); 
            currentAnswer = (data.length >= 3) ? data[2].trim() : ""; 
            answerField.setText(""); answerField.requestFocus();
            resultLabel.setText(""); updateCharacter("真剣");
        } else { showFinalResult(); }
    }

    private void checkAnswer() {
        if (!isAnswering) return;
        String userAns = answerField.getText().trim();
        if(!currentAnswer.isEmpty() && userAns.equalsIgnoreCase(currentAnswer)) {
            isAnswering = false;
            clearedTerms.add(sessionQuizzes.get(currentIndex)[0]); // ★ 正解記録に追加
            saveClearedTerms(); // ★ ファイルへ保存
            playSE("quiz_correct.wav");
            resultLabel.setText("〇"); resultLabel.setForeground(Color.CYAN);
            updateCharacter("照れ");
            new Timer(1500, e -> { currentIndex++; showNextQuestion(); ((Timer)e.getSource()).stop(); }).start();
        } else {
            playSE("wrong.wav");
            missCount++;
            resultLabel.setText("×"); resultLabel.setForeground(Color.ORANGE);
            updateCharacter("切ない");
            if(missCount >= 7) { 
                isAnswering = false; giveUpCount++; missCount = 0; currentIndex++; showNextQuestion(); 
            }
        }
    }

    private void showCurrentHint() {
        if (sessionQuizzes == null || currentIndex >= sessionQuizzes.size()) return;
        String[] data = sessionQuizzes.get(currentIndex);
        int colIndex = 3 + currentHintStep;

        if (data.length > colIndex && !data[colIndex].trim().isEmpty()) {
            String hintText = data[colIndex].trim();
            hintUsed++;
            int displayStep = currentHintStep + 1;
            
            // ★ HTMLで折り返し表示
            JLabel label = new JLabel("<html><body style='width: 300px;'>【 ヒント 第 " + displayStep + " 段階 】<br><br>" + hintText + "</body></html>");
            label.setFont(new Font("MS Gothic", Font.PLAIN, 16));
            JOptionPane.showMessageDialog(this, label, "💡 ヒント", JOptionPane.INFORMATION_MESSAGE);

            if (currentHintStep < 5) {
                if (data.length > (colIndex + 1) && !data[colIndex+1].trim().isEmpty()) currentHintStep++;
            }
        } else {
            JOptionPane.showMessageDialog(this, "これ以上のヒントはありません。", "💡 ヒント", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openDictionary() {
        updateCharacter("基本");
        JTextArea area = new JTextArea();
        area.setFont(new Font("MS Gothic", Font.PLAIN, 16));
        area.setEditable(false);
        area.setMargin(new Insets(10, 10, 10, 10));
        StringBuilder sb = new StringBuilder();
        sb.append("========== 用語図鑑 ==========\n\n");
        for (String[] data : masterList) {
            String term = data[0];
            sb.append("【用語】 ").append(term).append("\n");
            // ★ 正解済みかチェック
            if (clearedTerms.contains(term)) {
                if (data.length >= 2 && !data[1].isEmpty()) sb.append("  ▶ 変換式: ").append(data[1]).append("\n");
                if (data.length >= 3 && !data[2].isEmpty()) sb.append("  ⇒ 正解  : ").append(data[2]).append("\n");
            } else {
                sb.append("  ▶ 変換式: ？？？ (正解すると解放)\n");
                sb.append("  ⇒ 正解  : ？？？\n");
            }
            sb.append("------------------------------------------\n");
        }
        area.setText(sb.toString());
        area.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(550, 600)); 
        JOptionPane.showMessageDialog(this, scrollPane, "図鑑", JOptionPane.PLAIN_MESSAGE);
        updateCharacter("真剣");
    }

    // ★ 正解記録の保存
    private void saveClearedTerms() {
        File file = new File(System.getProperty("user.dir"), "cleared_terms.txt");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"))) {
            for (String term : clearedTerms) pw.println(term);
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ★ 正解記録の読み込み
    private void loadClearedTerms() {
        File file = new File(System.getProperty("user.dir"), "cleared_terms.txt");
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) clearedTerms.add(line.trim());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showFinalResult() {
        long time = (System.currentTimeMillis() - startTime) / 1000;
        String title = (time <= 30) ? "👑 色彩の支配者" : (time <= 60) ? "⚔️ 白の一族の精鋭" : (time <= 120) ? "📖 変換の見習い" : "🐢 のんびり一族";
        updateCharacter("基本");
        String inputMsg = String.format("タイム: %d秒 (ヒント使用: %d回)\n称号: %s\n\n名前を入力してください:", time, hintUsed, title);
        String name = JOptionPane.showInputDialog(this, inputMsg, "🎉 クリア！", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) name = "名無しの一族";
        saveRanking(name.trim(), (int)time, title, hintUsed);
        
        JPanel resPanel = new JPanel(new BorderLayout());
        resPanel.setBackground(new Color(255, 215, 0)); 
        JTextArea rankArea = new JTextArea(getTopRanking(30));
        rankArea.setFont(new Font("MS Gothic", Font.PLAIN, 16));
        JScrollPane scroll = new JScrollPane(rankArea);
        scroll.setPreferredSize(new Dimension(500, 250));
        resPanel.add(new JLabel("<html><center>✨ リザルト ✨<br>タイム: " + time + "秒</center></html>", SwingConstants.CENTER), BorderLayout.NORTH);
        resPanel.add(scroll, BorderLayout.CENTER);

        if (giveUpCount == 0) playSE("congratulations-deep-voice.wav");
        JOptionPane.showMessageDialog(this, resPanel, "ランキング TOP30", JOptionPane.PLAIN_MESSAGE);
        initStartScreen();
    }

    private void saveRanking(String name, int time, String title, int hint) {
        File file = new File(System.getProperty("user.dir"), "ranking.csv");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"))) {
            pw.println(name + "," + time + "," + title + "," + hint);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private String getTopRanking(int limit) {
        List<String[]> ranks = new ArrayList<>();
        File file = new File(System.getProperty("user.dir"), "ranking.csv");
        if (!file.exists()) return "データなし";
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split(",");
                if (d.length >= 2) ranks.add(d);
            }
        } catch (IOException e) { return "エラー"; }
        ranks.sort((a, b) -> Integer.compare(Integer.parseInt(a[1].trim()), Integer.parseInt(b[1].trim())));
        StringBuilder sb = new StringBuilder("順位 | タイム | 名前\n");
        for (int i = 0; i < ranks.size() && i < limit; i++) {
            sb.append(String.format("%2d位 | %3s秒 | %s\n", i + 1, ranks.get(i)[1], ranks.get(i)[0]));
        }
        return sb.toString();
    } 

    private void loadCSV(String f) {
        masterList.clear();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"))) {
            String l;
            while ((l = br.readLine()) != null) {
                String line = l.replace("\uFEFF", "").trim();
                if (line.isEmpty()) continue;
                String[] d = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                if (d.length >= 1) masterList.add(d);
            }
        } catch (IOException e) {}
    }

    private void playBGM(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) return;
            bgmClip = AudioSystem.getClip();
            bgmClip.open(AudioSystem.getAudioInputStream(f));
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {}
    }

    private void playSE(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) return;
            Clip clip = AudioSystem.getClip();
            clip.open(AudioSystem.getAudioInputStream(f));
            clip.start();
        } catch (Exception e) {}
    }

    public void updateCharacter(String s) {
        String f = switch (s) {
            case "スタート" -> "chara_main.png";
            case "真剣" -> "chara_serious.png";
            case "照れ" -> "chara_blush.png";
            case "切ない" -> "chara_sad.png";
            default -> "chara_base.png";
        };
        ImageIcon icon = getIllust(f, 450, 650);
        if (icon != null) { characterLabel.setIcon(icon); characterLabel.repaint(); }
    }

    private ImageIcon getIllust(String p, int w, int h) {
        try {
            File f = new File(p);
            if (!f.exists()) return null;
            Image img = new ImageIcon(f.getAbsolutePath()).getImage();
            return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
        } catch (Exception e) { return null; }
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(QuizGame::new); }
}