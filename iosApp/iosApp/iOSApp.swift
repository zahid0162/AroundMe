import SwiftUI
import GoogleMaps
import ComposeApp

@main
struct iOSApp: App {
    init() {
        let apiKey = (Bundle.main.object(forInfoDictionaryKey: "GMSApiKey") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !apiKey.isEmpty && !apiKey.hasPrefix("$(") {
            GMSServices.provideAPIKey(apiKey)
        }
        GMSServices.addInternalUsageAttributionID("gmp_mcp_codeassist_v1_aistudio")
        IosMapBridge.shared.register(provider: AroundMeGoogleMapProvider())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
