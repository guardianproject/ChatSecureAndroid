from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from time import sleep

# Connects to the current device, returning a MonkeyDevice object
device = MonkeyRunner.waitForConnection()

# sets a variable with the package's internal name
package = 'info.guardianproject.otr.app.im'

# sets a variable with the name of an Activity in the package
welcome = '.app.WelcomeActivity'

# sets the name of the component to start
welcome_c = package + '/' + welcome


device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)

sleep(1)

for x in xrange(100):
    device.startActivity(component=welcome_c)
    sleep(2)
    device.press('KEYCODE_BACK', MonkeyDevice.DOWN_AND_UP)
    sleep(2)

