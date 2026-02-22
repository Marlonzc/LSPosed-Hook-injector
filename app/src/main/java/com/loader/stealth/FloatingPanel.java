1| package com.loader.stealth;
2| 
3| import android.animation.ArgbEvaluator;
4| import android.animation.ValueAnimator;
5| import android.app.Activity;
6| import android.content.Context;
7| import android.graphics.Color;
8| import android.graphics.drawable.GradientDrawable;
9| import android.view.Gravity;
10| import android.view.View;
11| import android.view.ViewGroup;
12| import android.widget.ArrayAdapter;
13| import android.widget.Button;
14| import android.widget.CheckBox;
15| import android.widget.EditText;
16| import android.widget.FrameLayout;
17| import android.widget.LinearLayout;
18| import android.widget.Spinner;
19| import android.widget.TextView;
20| import android.widget.Toast;
21| 
22| import java.io.ByteArrayOutputStream;
23| import java.io.File;
24| import java.io.FileInputStream;
25| import java.io.FileNotFoundException;
26| import java.io.IOException;
27| import java.io.InputStream;
28| import java.util.ArrayList;
29| import java.util.Arrays;
30| import java.util.List;
31| 
32| public class FloatingPanel {
33|     private static final String DEFAULT_PATH = "/data/local/tmp/libvirtual.so";
34|     private static final List<String> PRESET_PATHS = Arrays.asList(
35|             DEFAULT_PATH,
36|             "/sdcard/Download/libvirtual.so",
37|             "/sdcard/Documents/libvirtual.so",
38|             "/sdcard/libvirtual.so"
39|     );
40| 
41|     public static void show(final Activity activity) {
42|         final FrameLayout root = new FrameLayout(activity);
43|         FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
44|                 ViewGroup.LayoutParams.MATCH_PARENT,
45|                 ViewGroup.LayoutParams.WRAP_CONTENT
46|         );
47|         rootParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
48|         rootParams.setMargins(24, 24, 24, 48);
49| 
50|         final LinearLayout layout = new LinearLayout(activity);
51|         layout.setOrientation(LinearLayout.VERTICAL);
52|         layout.setGravity(Gravity.CENTER_HORIZONTAL);
53|         layout.setPadding(24, 20, 24, 20);
54| 
55|         GradientDrawable bg = new GradientDrawable();
56|         bg.setColor(Color.argb(200, 18, 18, 20));
57|         bg.setCornerRadius(22f);
58|         bg.setStroke(2, Color.argb(160, 90, 90, 120));
59|         layout.setBackground(bg);
60| 
61|         // Top breathing banner
62|         TextView banner = new TextView(activity);
63|         banner.setText("tg：@USABullet520");
64|         banner.setTextSize(16);
65|         banner.setPadding(4, 4, 4, 16);
66|         banner.setGravity(Gravity.CENTER);
67|         banner.setTextColor(Color.parseColor("#9CE5FF"));
68|         layout.addView(banner, new LinearLayout.LayoutParams(
69|                 ViewGroup.LayoutParams.MATCH_PARENT,
70|                 ViewGroup.LayoutParams.WRAP_CONTENT
71|         ));
72| 
73|         // Address presets spinner + file picker
74|         LinearLayout rowTop = new LinearLayout(activity);
75|         rowTop.setOrientation(LinearLayout.HORIZONTAL);
76|         rowTop.setGravity(Gravity.CENTER_VERTICAL);
77|         rowTop.setPadding(0, 0, 0, 12);
78| 
79|         final Spinner pathSpinner = new Spinner(activity);
80|         final List<String> paths = new ArrayList<>(PRESET_PATHS);
81|         ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
82|                 android.R.layout.simple_spinner_dropdown_item, paths);
83|         pathSpinner.setAdapter(adapter);
84|         LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
85|         spLp.setMargins(0, 0, 12, 0);
86|         pathSpinner.setLayoutParams(spLp);
87| 
88|         Button pickBtn = new Button(activity);
89|         pickBtn.setText("选择 .so");
90|         pickBtn.setAllCaps(false);
91|         pickBtn.setTextColor(Color.WHITE);
92|         pickBtn.setBackgroundColor(Color.parseColor("#455A8E"));
93| 
94|         // Manual path input (kept for flexibility)
95|         final EditText pathInput = new EditText(activity);
96|         pathInput.setHint(DEFAULT_PATH);
97|         pathInput.setText(DEFAULT_PATH);
98|         pathInput.setTextColor(Color.parseColor("#C8FACC"));
99|         pathInput.setHintTextColor(Color.parseColor("#88C6A0"));
100|         pathInput.setTextSize(12);
101|         pathInput.setSingleLine(true);
102|         LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
103|                 ViewGroup.LayoutParams.MATCH_PARENT,
104|                 ViewGroup.LayoutParams.WRAP_CONTENT);
105|         lp.setMargins(0, 0, 0, 12);
106|         pathInput.setLayoutParams(lp);
107| 
108|         pickBtn.setOnClickListener(v -> {
109|             List<String> found = findSoFiles();
110|             if (found.isEmpty()) {
111|                 Toast.makeText(activity, "未找到 .so，尝试放到 Download 后再试", Toast.LENGTH_SHORT).show();
112|                 return;
113|             }
114|             String[] items = found.toArray(new String[0]);
115|             android.app.AlertDialog.Builder b = new android.app.AlertDialog.Builder(activity);
116|             b.setTitle("选择 .so 文件");
117|             b.setItems(items, (d, which) -> {
118|                 String sel = items[which];
119|                 addPathIfAbsent(paths, adapter, sel);
120|                 pathSpinner.setSelection(paths.indexOf(sel));
121|                 pathInput.setText(sel);
122|             });
123|             b.setNegativeButton("取消", null);
124|             b.show();
125|         });
126| 
127|         rowTop.addView(pathSpinner);
128|         rowTop.addView(pickBtn);
129|         layout.addView(rowTop);
130| 
131|         // Inject options row: checkbox + button
132|         LinearLayout rowBottom = new LinearLayout(activity);
133|         rowBottom.setOrientation(LinearLayout.HORIZONTAL);
134|         rowBottom.setGravity(Gravity.CENTER_VERTICAL);
135| 
136|         CheckBox selinuxToggle = new CheckBox(activity);
137|         selinuxToggle.setText("允许切换 SELinux（仅在失败时）");
138|         selinuxToggle.setTextColor(Color.parseColor("#C8FACC"));
139|         selinuxToggle.setChecked(true);
140|         LinearLayout.LayoutParams cbLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
141|         cbLp.setMargins(0, 0, 12, 0);
142|         selinuxToggle.setLayoutParams(cbLp);
143|         rowBottom.addView(selinuxToggle);
144| 
145|         Button btn = new Button(activity);
146|         btn.setText("注入");
147|         btn.setAllCaps(false);
148|         btn.setTextColor(Color.WHITE);
149|         btn.setBackgroundColor(Color.parseColor("#3A8FB7"));
150|         btn.setOnClickListener(v -> {
151|             if (!v.isEnabled()) return;
152|             String path = pathInput.getText().toString().trim();
153|             if (path.isEmpty()) {
154|                 path = (String) pathSpinner.getSelectedItem();
155|             }
156|             if (path == null || path.isEmpty()) path = DEFAULT_PATH;
157|             v.setEnabled(false);
157|             inject(activity, layout, path, v, selinuxToggle.isChecked());
158|         });
159| 
160|         LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(
161|                 ViewGroup.LayoutParams.WRAP_CONTENT,
162|                 ViewGroup.LayoutParams.WRAP_CONTENT);
163|         rowBottom.addView(btn, btnLp);
164|         layout.addView(pathInput);
165|         layout.addView(rowBottom);
166| 
167|         root.addView(layout);
168|         FrameLayout decorView = (FrameLayout) activity.getWindow().getDecorView();
169|         decorView.addView(root, rootParams);
170| 
171|         // Sync spinner selection with text input
172|         pathSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
173|             @Override
174|             public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
175|                 String sel = paths.get(position);
176|                 pathInput.setText(sel);
177|             }
178|             @Override
179|             public void onNothingSelected(android.widget.AdapterView<?> parent) { }
180|         });
181| 
182|         // Breathing effect
183|         startBreathing(banner, btn, pickBtn);
184|     }
185| 
186|     private static void addPathIfAbsent(List<String> paths, ArrayAdapter<String> adapter, String p) {
187|         if (!paths.contains(p)) {
188|             paths.add(0, p);
189|             adapter.notifyDataSetChanged();
190|         }
191|     }
192| 
193|     private static void startBreathing(TextView... views) {
194|         int[] colors = new int[]{
195|                 Color.parseColor("#8AE8FF"),
196|                 Color.parseColor("#FF7AF0"),
197|                 Color.parseColor("#9CFFA8")
198|         };
199|         ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
200|         animator.setDuration(2400);
201|         animator.setRepeatCount(ValueAnimator.INFINITE);
202|         animator.setRepeatMode(ValueAnimator.REVERSE);
203|         animator.addUpdateListener(a -> {
204|             float f = (float) a.getAnimatedValue();
205|             int idx = (int) (f * (colors.length - 1));
206|             int c1 = colors[idx];
207|             int c2 = colors[Math.min(idx + 1, colors.length - 1)];
208|             int color = (Integer) new ArgbEvaluator().evaluate(f - idx, c1, c2);
209|             for (TextView tv : views) {
210|                 if (tv != null) tv.setTextColor(color);
211|             }
212|         });
213|         animator.start();
214|     }
215| 
216|     private static List<String> findSoFiles() {
217|         List<String> found = new ArrayList<>();
218|         List<File> roots = Arrays.asList(
219|                 new File("/sdcard/Download"),
220|                 new File("/sdcard/Documents"),
221|                 new File("/sdcard"),
222|                 new File("/data/local/tmp")
223|         );
224|         for (File dir : roots) {
225|             if (dir.exists() && dir.isDirectory()) {
226|                 File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".so"));
227|                 if (files != null) {
228|                     for (File f : files) {
229|                         found.add(f.getAbsolutePath());
230|                     }
231|                 }
232|             }
233|         }
234|         return found;
235|     }
236| 
237|     private static void inject(Activity act, View layout, String path, View trigger, boolean allowSelinuxSwitch) {
238|         String prevState = null;
239|         boolean disabledSelinux = false;
240|         try {
241|             // First attempt: no SELinux change
242|             byte[] bytes = loadSoBytes(act, path);
243|             String res = NativeLoader.memfdInject(bytes);
244|             Toast.makeText(act, "结果: " + res, Toast.LENGTH_SHORT).show();
245|             if ("SUCCESS".equals(res)) {
246|                 new File(path).delete();
247|                 removePanel(act, layout);
248|                 return;
249|             }
250| 
251|             // Second attempt only if allowed
252|             if (allowSelinuxSwitch) {
253|                 prevState = getSelinuxState();
254|                 if (!"permissive".equalsIgnoreCase(prevState)) {
255|                     if (setSelinuxState("0")) {
256|                         disabledSelinux = true;
257|                     } else {
258|                         Toast.makeText(act, "关闭 SELinux 失败，可能无 root", Toast.LENGTH_SHORT).show();
259|                     }
260|                 }
261|                 res = NativeLoader.memfdInject(bytes);
262|                 Toast.makeText(act, "二次结果: " + res, Toast.LENGTH_SHORT).show();
263|                 if ("SUCCESS".equals(res)) {
264|                     new File(path).delete();
265|                     removePanel(act, layout);
266|                     return;
267|                 }
268|             }
269|         } catch (Exception e) {
270|             Toast.makeText(act, "失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
271|         } finally {
272|             if (disabledSelinux && prevState != null && !prevState.isEmpty()) {
273|                 setSelinuxState(prevState.equalsIgnoreCase("permissive") ? "0" : "1");
274|             }
275|             trigger.setEnabled(true);
276|         }
277|     }
278| 
279|     private static boolean setSelinuxState(String state) {
280|         return runShell("su", "-c", "setenforce " + state)
281|                 || runShell("setenforce " + state);
282|     }
283| 
284|     private static String getSelinuxState() {
285|         String res = runShellForOutput("getenforce");
286|         if (res == null) return "";
287|         return res.trim();
288|     }
289| 
290|     private static boolean runShell(String... cmd) {
291|         try {
292|             Process p = new ProcessBuilder(cmd)
293|                     .redirectErrorStream(true)
294|                     .start();
295|             int code = p.waitFor();
296|             return code == 0;
297|         } catch (Exception ignored) {
298|             return false;
299|         }
300|     }
301| 
302|     private static String runShellForOutput(String... cmd) {
303|         try {
304|             Process p = new ProcessBuilder(cmd)
305|                     .redirectErrorStream(true)
306|                     .start();
307|             ByteArrayOutputStream baos = new ByteArrayOutputStream();
308|             byte[] buf = new byte[1024];
309|             try (InputStream is = p.getInputStream()) {
310|                 int n;
311|                 while ((n = is.read(buf)) != -1) {
312|                     baos.write(buf, 0, n);
313|                 }
314|             }
315|             p.waitFor();
316|             return baos.toString();
317|         } catch (Exception ignored) {
318|             return null;
319|         }
320|     }
321| 
322|     private static byte[] loadSoBytes(Context ctx, String path) throws IOException {
323|         File file = new File(path);
324|         IOException lastError = null;
325| 
326|         if (file.exists() && file.isFile() && file.length() > 0) {
327|             try (FileInputStream fis = new FileInputStream(file)) {
328|                 return readAllBytes(fis, (int) file.length());
329|             } catch (SecurityException | IOException e) {
330|                 lastError = new IOException("读取失败: " + e.getMessage(), e);
331|             }
332|         } else {
333|             lastError = new FileNotFoundException("文件不存在或为空: " + path);
334|         }
335| 
336|         String assetName = file.getName();
337|         try (InputStream is = ctx.getAssets().open(assetName)) {
338|             return readAllBytes(is, -1);
339|         } catch (IOException ignored) {
340|         }
341| 
342|         if (lastError != null) throw lastError;
343|         throw new FileNotFoundException("未找到可读文件，也未在 assets 中找到: " + assetName);
344|     }
345| 
346|     private static byte[] readAllBytes(InputStream is, int expectedSize) throws IOException {
347|         if (expectedSize >= 0) {
348|             byte[] buf = new byte[expectedSize];
349|             int read = is.read(buf);
350|             if (read != expectedSize) {
351|                 throw new IOException("读取长度不一致: " + read + " vs " + expectedSize);
352|             }
353|             return buf;
354|         }
355|         ByteArrayOutputStream baos = new ByteArrayOutputStream();
356|         byte[] temp = new byte[16 * 1024];
357|         int n;
358|         while ((n = is.read(temp)) != -1) {
359|             baos.write(temp, 0, n);
360|         }
361|         return baos.toByteArray();
362|     }
363| 
364|     private static void removePanel(Activity act, View layout) {
365|         View decor = act.getWindow().getDecorView();
366|         if (decor instanceof FrameLayout) {
367|             ((FrameLayout) decor).removeView(layout.getParent() instanceof View ? (View) layout.getParent() : layout);
368|         }
369|     }
370| }