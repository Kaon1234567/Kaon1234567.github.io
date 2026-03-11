import java.awt.*;
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
    private int currentIndex = 0, missCount = 0, hintUsed = 0, giveUpCount = 0;
    private int currentHintStep = 0; 
    private long startTime;
    private String currentAnswer = "";
    private boolean isAnswering = true;
    private Clip bgmClip;

    public QuizGame() {
        setTitle("用語変換");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 800);
        setLayout(null);
        getContentPane().setBackground(new Color(20, 20, 25));

        loadCSV("quiz_data3.csv");
        playBGM("bgm.wav");

        characterLabel = new JLabel();
        characterLabel.setBounds(620, 50, 450, 650);
        add(characterLabel);

        titleLabel = new JLabel("用語変換", SwingConstants.CENTER);
        titleLabel.setBounds(50, 150, 550, 100);
        titleLabel.setFont(new Font("MS Gothic", Font.BOLD, 72));
        titleLabel.setForeground(new Color(255, 215, 0));
        
        startButton = new JButton("START");
        startButton.setBounds(225, 350, 200, 80);
        startButton.setFont(new Font("Arial", Font.BOLD, 30));
        startButton.addActionListener(e -> startGame());
        
        questionLabel = new JLabel("", SwingConstants.CENTER);
        questionLabel.setBounds(20, 80, 580, 300);
        questionLabel.setFont(new Font("MS Gothic", Font.BOLD, 32));
        questionLabel.setForeground(Color.WHITE);

        answerField = new JTextField();
        answerField.setBounds(150, 400, 250, 50);
        answerField.setFont(new Font("MS Gothic", Font.PLAIN, 24));
        answerField.addActionListener(e -> checkAnswer());

        submitButton = new JButton("送信");
        submitButton.setBounds(410, 400, 100, 50);
        submitButton.addActionListener(e -> checkAnswer());

        resultLabel = new JLabel("", SwingConstants.CENTER);
        resultLabel.setBounds(50, 300, 550, 80);
        resultLabel.setFont(new Font("MS Gothic", Font.BOLD, 40));

        dictButton = new JButton("図鑑");
        dictButton.setBounds(20, 700, 100, 40);
        dictButton.addActionListener(e -> openDictionary());

        hintButton = new JButton("ヒント");
        hintButton.setBounds(130, 700, 100, 40);
        hintButton.addActionListener(e -> showCurrentHint());

        resetButton = new JButton("リセット");
        resetButton.setBounds(240, 700, 100, 40);
        resetButton.addActionListener(e -> initStartScreen());

        add(titleLabel); add(startButton); add(questionLabel);
        add(answerField); add(submitButton); add(resultLabel);
        add(dictButton); add(hintButton); add(resetButton);

        initStartScreen();
        setLocationRelativeTo(null);
        setVisible(true);
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
            answerField.setText("");
            answerField.requestFocus();
            resultLabel.setText("");
            updateCharacter("真剣");
        } else { showFinalResult(); }
    }

    private void checkAnswer() {
        if (!isAnswering) return;
        String userAns = answerField.getText().trim();
        if(!currentAnswer.isEmpty() && userAns.equalsIgnoreCase(currentAnswer)) {
            isAnswering = false;
            playSE("quiz_correct.wav");
            resultLabel.setText("〇");
            resultLabel.setForeground(Color.CYAN);
            updateCharacter("照れ");
            new Timer(1500, e -> { currentIndex++; showNextQuestion(); ((Timer)e.getSource()).stop(); }).start();
        } else {
            playSE("wrong.wav");
            missCount++;
            resultLabel.setText("×");
            resultLabel.setForeground(Color.ORANGE);
            updateCharacter("切ない");
            if(missCount >= 7) { 
                isAnswering = false; giveUpCount++; missCount = 0; currentIndex++; showNextQuestion(); 
            }
        }
    }

private void showCurrentHint() {
        if (sessionQuizzes == null || currentIndex >= sessionQuizzes.size()) return;
        String[] data = sessionQuizzes.get(currentIndex);
        
        // ★ 3(ヒント1) からスタートして、段階(0,1,2...)を足す
        int colIndex = 3 + currentHintStep;

        if (data.length > colIndex && !data[colIndex].trim().isEmpty()) {
            String hintText = data[colIndex].trim();
            hintUsed++;
            
            // ★ 「表示する段階」を先に進めてからダイアログを出す
            // これで「第1段階」の時に「ヒント1」が正しく出ます
            int displayStep = currentHintStep + 1;

            JOptionPane.showMessageDialog(this, 
                "【 ヒント 第 " + displayStep + " 段階 】\n\n" + hintText, 
                "💡 ヒント", JOptionPane.INFORMATION_MESSAGE);

            // 次回のためにカウントアップ（最大6段階まで）
            if (currentHintStep < 5) {
                // 次の列が存在する場合のみ次へ進む
                if (data.length > (colIndex + 1) && !data[colIndex+1].trim().isEmpty()) {
                    currentHintStep++;
                }
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
            if (data.length >= 1) {
                sb.append("【用語】 ").append(data[0]).append("\n");
                if (data.length >= 2 && !data[1].isEmpty()) sb.append("  ▶ 変換式: ").append(data[1]).append("\n");
                if (data.length >= 3 && !data[2].isEmpty()) sb.append("  ⇒ 正解  : ").append(data[2]).append("\n");
                sb.append("------------------------------------------\n");
            }
        }
        area.setText(sb.toString());
        area.setCaretPosition(0);
        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(550, 600)); 
        JOptionPane.showMessageDialog(this, scrollPane, "図鑑", JOptionPane.PLAIN_MESSAGE);
        updateCharacter("真剣");
    }

    private void showFinalResult() {
        long time = (System.currentTimeMillis() - startTime) / 1000;
        String title = (time <= 30) ? "👑 色彩の支配者" : (time <= 60) ? "⚔️ 白の一族の精鋭" : (time <= 120) ? "📖 変換の見習い" : "🐢 のんびり一族";

        updateCharacter("基本");
        
        // 1. 名前入力
        String inputMsg = String.format("タイム: %d秒 (ヒント使用: %d回)\n称号: %s\n\n名前を入力してください:", time, hintUsed, title);
        String name = JOptionPane.showInputDialog(this, inputMsg, "🎉 クリア！", JOptionPane.QUESTION_MESSAGE);
        if (name == null || name.trim().isEmpty()) name = "名無しの一族";

        // 2. 保存と取得（エラーが起きても止まらないように try-catch で囲む）
        String topRanking = "ランキングを読み込めませんでした。";
        try {
            saveRanking(name.trim(), (int)time, title, hintUsed);
            topRanking = getTopRanking(30);
        } catch (Exception e) {
            System.err.println("Ranking Error: " + e.getMessage());
        }

        // 3. 表示用パネルの作成
        JPanel resPanel = new JPanel(new BorderLayout());
        resPanel.setBackground(new Color(255, 215, 0)); 

        // キャラ画像
        JPanel charaPanel = new JPanel(new GridLayout(1, 5, 5, 0));
        charaPanel.setBackground(new Color(255, 215, 0));
        String[] charas = {"chara_main.png", "chara_serious.png", "chara_blush.png", "chara_sad.png", "chara_base.png"};
        for (String c : charas) {
            ImageIcon icon = getIllust(c, 160, 240);
            if (icon != null) charaPanel.add(new JLabel(icon));
        }

        // ランキングテキスト
        JTextArea rankArea = new JTextArea(topRanking);
        rankArea.setFont(new Font("MS Gothic", Font.PLAIN, 16));
        rankArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(rankArea);
        scroll.setPreferredSize(new Dimension(500, 250));

        resPanel.add(charaPanel, BorderLayout.NORTH);
        resPanel.add(new JLabel("<html><center><font size='5'>✨ リザルト ✨</font><br>タイム: " + time + "秒 (ヒント: " + hintUsed + "回)</center></html>", SwingConstants.CENTER), BorderLayout.CENTER);
        resPanel.add(scroll, BorderLayout.SOUTH);

        // 4. 音声再生
        if (giveUpCount == 0) playSE("congratulations-deep-voice.wav");

        // 5. ダイアログ表示（ここが出るはず！）
        JOptionPane.showMessageDialog(this, resPanel, "ランキング TOP30", JOptionPane.PLAIN_MESSAGE);

        // 6. 強制リセット
        initStartScreen();
        this.repaint();
    }

    private void saveRanking(String name, int time, String title, int hint) {
        // カレントディレクトリの絶対パスからファイルを作成
        File file = new File(System.getProperty("user.dir"), "ranking.csv");
        
        // UTF-8で追記モード。リソース自動閉鎖(try-with-resources)を使用
        try (FileOutputStream fos = new FileOutputStream(file, true);
             OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
             PrintWriter pw = new PrintWriter(osw)) {
            
            pw.println(name + "," + time + "," + title + "," + hint);
            pw.flush(); // 強制的に書き込みを確定
        } catch (IOException e) {
            // ここでスタック・トレースが出る場合は、コンソールを確認してください
            e.printStackTrace();
        }
    }

    private String getTopRanking(int limit) {
        List<String[]> ranks = new ArrayList<>();
        File file = new File(System.getProperty("user.dir"), "ranking.csv");
        
        if (!file.exists()) return "まだデータがありません。";

        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
             BufferedReader br = new BufferedReader(isr)) {
            
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] d = line.split(",");
                if (d.length >= 2) ranks.add(d);
            }
        } catch (IOException e) {
            return "ランキング読み込みエラー";
        }

        // タイムで並び替え
        ranks.sort((a, b) -> {
            try {
                return Integer.compare(Integer.parseInt(a[1].trim()), Integer.parseInt(b[1].trim()));
            } catch (Exception e) { return 0; }
        });

        StringBuilder sb = new StringBuilder(" 順位 | タイム | ヒント | 名前 | 称号\n--------------------------------------------------\n");
        for (int i = 0; i < ranks.size() && i < limit; i++) {
            String[] r = ranks.get(i);
            String h = (r.length > 3) ? r[3] : "0";
            String t = (r.length > 2) ? r[2] : "なし";
            sb.append(String.format(" %2d位 | %3s秒 | %2s回 | %s | %s\n", i + 1, r[1], h, r[0], t));
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
                for (int i = 0; i < d.length; i++) d[i] = d[i].trim().replaceAll("^\"|\"$", "");
                if (d.length >= 1) masterList.add(d);
            }
        } catch (IOException e) {}
    }

    private void playBGM(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // マルチキャッチで警告解消
        }
    }

    private void playSE(String filename) {
        try {
            File f = new File(filename);
            if (!f.exists()) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(f);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            // マルチキャッチで警告解消
        }
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