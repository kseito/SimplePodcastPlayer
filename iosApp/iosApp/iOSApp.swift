import SwiftUI
import AVFoundation

@main
struct iOSApp: App {
    init() {
        // Setup audio session for background playback
        setupAudioSession()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func setupAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()

        do {
            // Set category to playback to enable background audio
            try audioSession.setCategory(.playback, mode: .default)

            // Activate the audio session
            try audioSession.setActive(true)

            print("Audio session configured for background playback")
        } catch {
            print("Failed to setup audio session: \(error.localizedDescription)")
        }
    }
}