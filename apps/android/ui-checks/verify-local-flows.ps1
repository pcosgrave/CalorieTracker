param([string[]]$Labels, [string]$Screenshot, [hashtable]$Fields)
$adb = 'C:\Users\Philip\AppData\Local\Android\Sdk\platform-tools\adb.exe'
function Read-Ui {
    $snapshotPath = '/sdcard/bitewise-check-' + [guid]::NewGuid().ToString('N') + '.xml'
    & $adb -s emulator-5554 shell uiautomator dump $snapshotPath | Out-Null
    [xml]$tree = (& $adb -s emulator-5554 shell cat $snapshotPath)
    if (-not $tree.hierarchy) { throw 'Emulator did not produce a fresh UI snapshot.' }
    return $tree
}
foreach ($label in $Labels) {
    $tree = Read-Ui
    $node = $tree.SelectNodes('//node[@text!=""]') | Where-Object { $_.text -eq $label } | Select-Object -First 1
    if (-not $node) { throw "Missing UI text: $label" }
    $values = [regex]::Matches($node.bounds,'\d+') | ForEach-Object { [int]$_.Value }
    & $adb -s emulator-5554 shell input tap ([int](($values[0]+$values[2])/2)) ([int](($values[1]+$values[3])/2))
}
foreach ($key in $Fields.Keys) {
    $tree = Read-Ui
    $node = $tree.SelectNodes('//node[@class="android.widget.EditText"]') | Where-Object { @($_.SelectNodes('.//node[@text!=""]') | Where-Object { $_.text -eq $key }).Count -gt 0 } | Select-Object -First 1
    if (-not $node) { throw "Missing field: $key" }
    $values = [regex]::Matches($node.bounds,'\d+') | ForEach-Object { [int]$_.Value }
    & $adb -s emulator-5554 shell input tap ([int](($values[0]+$values[2])/2)) ([int](($values[1]+$values[3])/2))
    & $adb -s emulator-5554 shell input keycombination 113 29
    & $adb -s emulator-5554 shell input keyevent 67
    $inputValue = [string]$Fields[$key]
    if ($inputValue -notmatch '^[a-zA-Z0-9 .]+$') { throw 'Test field text must be alphanumeric.' }
    & $adb -s emulator-5554 shell input text ($inputValue.Replace(' ', '%s'))
    & $adb -s emulator-5554 shell input keyevent 4
}
$tree = Read-Ui
$tree.SelectNodes('//node[@text!=""]') | ForEach-Object { "$($_.text) $($_.bounds)" }
if ($Screenshot) {
    & $adb -s emulator-5554 shell screencap -p /sdcard/bitewise-verification.png
    & $adb -s emulator-5554 pull /sdcard/bitewise-verification.png $Screenshot
}
