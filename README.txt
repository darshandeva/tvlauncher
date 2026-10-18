HomeTV - lightweight Apple TV-style launcher (Kotlin, no Compose, one dependency)

BUILD WITHOUT ANDROID STUDIO (GitHub Actions)
1. Create a free GitHub repo and upload ALL files in this folder, keeping the folder structure.
   Make sure .github/workflows/build.yml exists in the repo. If your browser hid the
   dot-folder: Add file > Create new file, name it  .github/workflows/build.yml  and paste
   the contents of build.yml.
2. Open the Actions tab > "Build APK" > Run workflow. Wait about 3-5 minutes.
3. Open the finished run, download the "HomeTV-apk" artifact, unzip it -> app-release.apk

INSTALL ON THE BOX
  adb connect <box-ip>:5555
  adb install app-release.apk
Press Home and pick "Home" > Always. If it isn't offered:
  adb shell cmd package set-home-activity com.dev.hometv/.MainActivity
Go back to stock:
  adb uninstall com.dev.hometv

USE
  Hold OK on any app to add / remove it from Favorites.
  The Home button returns to the first favorite.
