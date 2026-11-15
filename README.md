# স্ক্রিন টাইম ট্র্যাকার (Screen Time Tracker)

সম্পূর্ণ অফলাইন ও বাংলা ভাষার Android অ্যাপ টাইমার। নির্বাচিত কোনো অ্যাপ (যেমন Facebook, YouTube) খোলার আগে কাজের উদ্দেশ্য ও সময় (১–১৮০ মিনিট) নির্ধারণ করে, বিরতি ও পুনরায় বাকি সময় থেকে গণনা ধরে রাখে এবং মোট সময় শেষ হলে স্বয়ংক্রিয়ভাবে হোম স্ক্রিনে ফিরিয়ে আনে।

---

## ফোনে সরাসরি APK ডাউনলোড ও ইনস্টল করার নিয়ম

কোনো কম্পিউটার বা Android Studio ছাড়াই সরাসরি GitHub থেকে ফোনে APK ডাউনলোড করা যাবে:

1. এই GitHub রিপোজিটরির ডানপাশে (বা ফোনের ব্রাউজারে নিচে স্ক্রল করে) **Releases** অংশে যান।
2. **"স্ক্রিন টাইম ট্র্যাকার (সর্বশেষ APK)"** রিলিজটি খুলুন।
3. **Assets** তালিকায় থাকা **`ScreenTimeTracker.apk`** ফাইলটিতে ট্যাপ করে ডাউনলোড করুন।
4. ডাউনলোড শেষে ফাইলটিতে ট্যাপ করে **Install** করুন (*Unknown sources / Install unknown apps* অনুমতি চাইলে Allow করুন)।

---

## GitHub-এ পাবলিশ করার পর স্বয়ংক্রিয়ভাবে APK তৈরি হওয়ার নিয়ম

- এই প্রজেক্টে `.github/workflows/build-apk.yml` যুক্ত করা আছে।
- আপনি GitHub-এ কোড **Push** করলেই ২–৪ মিনিটের মধ্যে GitHub Actions স্বয়ংক্রিয়ভাবে `ScreenTimeTracker.apk` তৈরি করে **Releases** সেকশনে আপলোড করে দেবে।
- কোনো পাসওয়ার্ড বা আলাদা Keystore তৈরি করার প্রয়োজন নেই।
- চাইলে GitHub-এর **Actions > Build & Publish Android APK > Run workflow** থেকেও যেকোনো সময় নতুন করে APK তৈরি করা যাবে।

---

## প্রথমবার ফোনে চালানোর নিয়ম (Symphony Z42 / Android 11)

1. অ্যাপ খুলে ধাপে ধাপে **৫টি অনুমতি** চালু করুন (Accessibility Service, Display over other apps, Usage Access, Notifications, Battery Optimization exemption)।
2. Symphony Z42 বা বাজেট Android ফোনে `Settings > Apps & notifications > স্ক্রিন টাইম ট্র্যাকার > Battery > Don't optimize` নির্বাচন করে রাখুন এবং Recents থেকে অ্যাপটি লক করে রাখুন।
3. **অ্যাপ** ট্যাব থেকে যে অ্যাপগুলো নিয়ন্ত্রণ করতে চান সেগুলোর সুইচ অন করুন।
