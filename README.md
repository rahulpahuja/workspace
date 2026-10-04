# Mobile1X Workspace Manager

A small, local-only Kotlin/JVM desktop app for launching your Mobile1X work environment on macOS.

It can launch:

- Firebase
- Google Analytics
- Amplitude
- ChatGPT
- Claude
- Mobile1X GitHub
- Any other website/URL
- Any macOS `.app`
- Terminal

It also supports enabling/disabling items, saving configuration locally, and configuring launch-at-login.

## Requirements

- macOS
- Java 17+ recommended
- Kotlin compiler
- Internet access for websites
- Google Chrome if you want to use the Chrome-profile feature

No paid service is required.

## 1. Install Java

Check whether Java is already installed:

```bash
java -version
```

If you do not have it, install a JDK. With Homebrew:

```bash
brew install openjdk@17
```

Then verify:

```bash
java -version
```

## 2. Install Kotlin

If Homebrew is installed:

```bash
brew install kotlin
```

Verify:

```bash
kotlinc -version
```

## 3. Put the Kotlin file somewhere convenient

For example:

```text
~/Desktop/workspace/
    src/
    config/
```

Open Terminal and go there:

```bash
cd ~/Desktop/workspace
```

## 4. Compile the application

Run:

```bash
kotlinc src -include-runtime -d Workspace.jar
```

This creates:

```text
Workspace.jar
```

## 5. Run it

```bash
java -jar Mobile1XWorkspace.jar
```

The Mobile1X Workspace Manager window should open.

---

# First-time setup

The application comes with several default entries:

- Firebase
- Google Analytics
- Amplitude
- ChatGPT
- Claude
- Mobile1X GitHub

You can enable or disable each one.

For example:

```text
Firebase          ✓
Google Analytics  ✓
Amplitude         ✓
ChatGPT           ✓
Claude            ✓
Mobile1X GitHub   ✓
```

Click **Launch Workspace** to open the enabled items.

---

# Adding another website

Use **Add Website**.

Enter:

```text
Name:
Supabase

URL:
https://supabase.com/
```

Save it.

The website will appear in your workspace and its enabled/disabled state will be saved.

Other examples:

```text
AWS Console
https://console.aws.amazon.com/

Figma
https://www.figma.com/

GitHub
https://github.com/

Play Console
https://play.google.com/console/

App Store Connect
https://appstoreconnect.apple.com/
```

---

# Adding a Mac application

Use **Add Application** and select an application such as:

```text
/Applications/Xcode.app
```

or:

```text
/Applications/Android Studio.app
```

The application can then be enabled or disabled like the websites.

---

# Terminal

If Terminal is enabled, the launcher opens Terminal as part of the workspace.

You can configure the working directory in the application settings.

For example:

```text
~/Mobile1X
```

or:

```text
~/Developer/Mobile1X
```

---

# Chrome profile

The launcher can open websites through a specific Chrome profile.

This is useful if you want a dedicated:

```text
Mobile1X
```

Chrome profile containing your authenticated sessions.

## Find your Chrome profile directory

Open Chrome with the Mobile1X profile.

Then open:

```text
chrome://version
```

Look for:

```text
Profile Path
```

You may see something similar to:

```text
/Users/rahul/Library/Application Support/Google/Chrome/Profile 3
```

The profile directory is:

```text
Profile 3
```

The **Chrome profile** dropdown in the main window lists every profile Chrome knows about, read from Chrome's own `Local State` file (`~/Library/Application Support/Google/Chrome/Local State`). Pick one and it is saved to the config.

Do not copy passwords or cookies into the application.

The application should only tell Chrome which profile to use.

---

# Authentication and security

The application does **not** need your passwords.

It should not store:

- Google passwords
- Amplitude passwords
- ChatGPT passwords
- Claude passwords
- Browser cookies
- Session tokens
- API keys

Authentication remains inside Chrome.

This means your normal Chrome authentication/session is reused rather than copied into the launcher.

For better security:

- Use a dedicated Chrome profile.
- Enable Google 2-Step Verification or a passkey.
- Keep macOS and Chrome updated.
- Only install trusted Chrome extensions.
- Keep your Mac locked when unattended.

---

# Saving configuration

All workspace data is loaded from a single config file, not from the code:

```text
~/Library/Application Support/Mobile1XWorkspace/workspace.properties
```

To start from scratch, copy `config/workspace.properties.example` to that path. The app reads and rewrites the file as you change items, the profile, or startup settings.

The application stores workspace configuration locally.

The configuration is intended to contain things such as:

```text
Application name
Application path
Website URL
Enabled/disabled state
Workspace settings
Startup preference
Chrome profile configuration
```

It should not contain passwords or browser session tokens.

---

# Launch at macOS login

The application supports configuring the workspace to start when you log into macOS.

When enabled, it uses a macOS `LaunchAgent`.

This is preferable to putting credentials or passwords into a shell script.

If macOS asks for permission during setup, review the requested permission before accepting it.

---

# Recommended workspace

For your Mobile1X workflow, I recommend starting with:

```text
✓ Firebase
✓ Google Analytics
✓ Amplitude
✓ ChatGPT
✓ Claude
✓ Mobile1X GitHub
✓ Terminal
```

Then add applications as needed:

```text
○ Android Studio
○ Xcode
○ Figma
○ Slack
○ Notion
○ Postman
○ Docker
○ Cursor
```

---

# Running it again later

After compilation, you only need:

```bash
cd ~/Mobile1XWorkspace
java -jar Mobile1XWorkspace.jar
```

You do **not** need to compile it again unless you change the Kotlin source.

---

# Creating a convenient launcher

You can create a small file called:

```text
Mobile1XWorkspace.command
```

with:

```bash
#!/bin/bash

cd "$(dirname "$0")"
java -jar Mobile1XWorkspace.jar
```

Make it executable:

```bash
chmod +x Mobile1XWorkspace.command
```

Now you can double-click:

```text
Mobile1XWorkspace.command
```

from Finder.

If macOS blocks it initially, right-click it and choose **Open**.

---

# Optional: create a shell command

You can also create an alias:

```bash
alias mobile1x='java -jar "$HOME/Mobile1XWorkspace/Mobile1XWorkspace.jar"'
```

Then:

```bash
mobile1x
```

opens the workspace manager.

For a permanent alias, add it to your shell configuration (`~/.zshrc` on a typical modern macOS installation).

---

# Troubleshooting

## `kotlinc: command not found`

Install Kotlin:

```bash
brew install kotlin
```

Then:

```bash
kotlinc -version
```

## `java: command not found`

Install Java:

```bash
brew install openjdk@17
```

Then verify:

```bash
java -version
```

## Website opens but you are asked to log in

This usually means Chrome opened a different profile.

Check:

```text
chrome://version
```

and configure the correct Chrome profile directory.

## An application does not launch

Check that the `.app` path still exists.

For example:

```bash
ls -ld "/Applications/Xcode.app"
```

## Startup does not work

Check the macOS LaunchAgent configuration and make sure the application has permission to run.

Also test the workspace manually first:

```bash
java -jar Mobile1XWorkspace.jar
```

---

# Important

This is a local desktop utility. It does not require:

- A server
- A database
- Cloud hosting
- An SSO provider
- An API subscription
- A paid automation service

Your workspace configuration stays on your Mac.

