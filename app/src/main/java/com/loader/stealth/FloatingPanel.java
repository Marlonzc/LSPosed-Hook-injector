import android.app.AlertDialog;

// Obfuscation helper
private static String x(int... a) { char[] c = new char[a.length]; for (int i=0;i<a.length;i++){ c[i]=(char)(a[i] ^ 0x55); } return new String(c); }

// Setenforce base array
private static final int[] setenforce = {38,48,33,48,59,51,58,39,54,48};
private static final int space = 117;
private static final String setenforceState = x(setenforce) + " " + x(new int[]{/* state */});

// Getenforce array
private static final int[] getenforce = {50,48,33,48,59,51,58,39,54,48};
private static final String getenforceState = x(getenforce);

// Adjusted containsPermDenied method
private boolean containsPermDenied(String permissions) {
    String[] parts = permissions.split("\+");
    boolean denied = false;
    for (String part : parts) {
        if (part.contains("mission") || part.contains("denied") || part.contains("sel") || part.contains("inux") || part.contains("eacces") || part.contains("appfileaccess")) {
            denied = true;
            break;
        }
    }
    return denied;
}

// Modify inject flow
private void modifyInjectFlow() {
    // Remove auto checkbox gating
    // On permissionIssue
    confirmationDialog("兼容模式", "检测到受限访问，是否尝试兼容模式（可能需要更高权限）？");
}

private void confirmationDialog(String title, String message) {
    new AlertDialog.Builder(context)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton("尝试", (dialog, which) -> {
            // Trigger SELinux toggle path
            setSelinuxState(setenforceState);
            secondInject();
        })
        .setNegativeButton("取消", (dialog, which) -> {
            // Do nothing
            dialog.dismiss();
        })
        .show();
}

// Ensure proper success handling
private void handleSuccess() {
    // Remove panel and keep sticky banner
    removePanel();
    keepStickyBanner();
}