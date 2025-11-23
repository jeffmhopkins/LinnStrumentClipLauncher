# LinnStrument Cliplauncher Script for Bitwig

This repository contains the source code for a controller extension that adds a clip launcher mode to LinnStrument. 

NEW: Added extra pages for: Game of Life, QWERTY Keyboard, Paint, and Flappy Bird!

## Installation

- Download the `LinnstrumentClipLauncher.bwextension` file and save it to:
    - Windows: `%USERPROFILE%\Documents\Bitwig Studio\Extensions\`
    - Mac: `~/Documents/Bitwig Studio/Extensions/`
    - Linux: `~/Bitwig Studio/Extensions/`
- Start Bitwig Studio and open the Dashboard. Select Settings and Controllers.
- If your LinnStrument is listed there already, remove it.
- add the LinnStrument clip launcher extension by clicking `Add controller manually`
- note the "User Button 2 CC" setting: Set up your LinnStrument to send MIDI on this MIDI CC when the user pushes the "Switch 2"
- adjust the other settings to your liking.
- The second MIDI output is the output for MIDI generated from the QWERTY keyboard, Life, or Flappy Birds

## How to use Clip Launcher

LinnStrument behaves just like you'd expect - except that "Switch 2" in the left row is now 
used to toggle between "normal operation" and "cliplauncher mode".

Press "Switch 2". If setup correctly, the normal note layout will disappear and you'll see a grid 
of your clips as well as navigation controls. The two green buttons in the corners navigate up and 
down - the outer button moves the view by one clip, the inner button moves the view by a whole page. 
The yellow buttons on the left and right edge navigate left and right. Here, the upper button moves 
by one track and the lower button by a whole page. On the right side there is also a vertical row of 
cyan buttons. These are your scenes. Press one of them to start playing the scene. All other buttons 
except for the low row are your clips. Press one of the clips to start playing it.

The low row can act in two modes: When the low row lights up red, it acts as a stop control that will 
stop the corresponding track. The red button right below the cyan column of scenes will stop everything.
Press the "Octave/Transpose" button on the left to change the low row mode to "track selection". Now the 
low row will be cyan with the currently selected track in blue.

The "Per Split Settings" button is your recording button. When pressed, it lights up red. When you now press
any of the clips bitwig will start recording to that clip.

The "Preset" button is your copy button. Press it and it will flash green. Press any clip that you want to 
copy, then press any position where you want to copy that clip to.

The "Volume" button is your delete button. Press it and it will flash red. Press any clip you want to delete, 
then press the "Volume" button again to exit delete mode.

Press "Switch 2" at any time to return back to normal LinnStrument operation.

## How to use Game of Life

The left hand buttons control things.
    // 0,0 - Start / Stop
    // 0,1 - Short Press Clear Cells, Long Press Clear Beacons
    // 0,2 - Tempo Sync Mode, toggles between DAW SYNC variations or 100ms
    // 0,3 - Display Mode, toggles between B/W, Warm, Cool, Magenta
    // 0,4 - Change Extension App (Clip launcher etc)
    // 0,5 - Exit back to LinnStrument
    // 0,6 - Auto-Seed, toggles between ON/OFF and sets the number of ticks between reseeding

When running, a button press on the grid will create cells, or kill them toggling their state.

When not running, a long press will create MIDI beacons (starting at midi note 60 and increasing with more beacons added). Beacons can be removed by long pressing them as well. Long pressing the clear grid button (0,1 ie 'preset' button) will also clear ALL beacons

You can change Tempo sync, Colors, and auto seed as well. 

## How to use QWERTY Keyabord

Each button is mapped to a MIDI note. Use Coyote MIDI to map translations for those through a virtual midi port to make the keyboard type.

## How to use Paint

Press a square to color. press 0,0 to open the color pallette. hold 0,1 and draw to erase

## How to play Flappy Birds

Any press on the screen will start the game. Fly between obstacles. It will send midi for each flap and for each death (on a different note)
