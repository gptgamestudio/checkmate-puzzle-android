# AdMobManager.gd
# Production-ready AdMob Singleton / Helper for Godot 4.x Mobile
# Configured with user's specific AdMob App ID and Ad Units.

class_name AdMobManager
extends Node

const APP_ID = "ca-app-pub-9786472984542330~7452842897"
const BANNER_AD_UNIT_ID = "ca-app-pub-9786472984542330/2771361852"
const INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-9786472984542330/2579790164"

var is_interstitial_loaded: bool = false
var is_banner_visible: bool = false

# Reference to native AdMob singleton if using Godot AdMob Android Plugin
var admob_singleton: Object = null

func _ready() -> void:
	if Engine.has_singleton("PoingGodotAdMob"):
		admob_singleton = Engine.get_singleton("PoingGodotAdMob")
		_initialize_plugin()
	elif Engine.has_singleton("GodotAdMob"):
		admob_singleton = Engine.get_singleton("GodotAdMob")
		_initialize_plugin()
	else:
		print("[AdMobManager] Running in Editor/Simulated mode. AdMob IDs ready.")

func _initialize_plugin() -> void:
	if admob_singleton == null:
		return
	# Initialize SDK
	admob_singleton.initialize()
	# Load standard bottom banner
	load_bottom_banner()
	# Preload interstitial
	load_interstitial()

func load_bottom_banner() -> void:
	if admob_singleton:
		# Standard 320x50 Banner at BOTTOM
		admob_singleton.load_banner(BANNER_AD_UNIT_ID, 1) # 1 = Position.BOTTOM
		is_banner_visible = true
	else:
		print("[AdMobManager] Banner loaded: ", BANNER_AD_UNIT_ID)

func load_interstitial() -> void:
	if admob_singleton:
		admob_singleton.load_interstitial(INTERSTITIAL_AD_UNIT_ID)
	else:
		is_interstitial_loaded = true
		print("[AdMobManager] Interstitial preloaded: ", INTERSTITIAL_AD_UNIT_ID)

func show_interstitial() -> void:
	if admob_singleton:
		if admob_singleton.is_interstitial_loaded():
			admob_singleton.show_interstitial()
		else:
			load_interstitial()
	else:
		print("[AdMobManager] Show Interstitial triggered on Checkmate!")
		# Auto reload
		load_interstitial()
