require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorNetsEasy'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = package['repository']['url']
  s.author = 'Caterflow'
  s.source = { :git => "#{package['repository']['url']}.git", :tag => s.version.to_s }
  s.source_files = 'ios/Sources/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.ios.deployment_target = '15.0'
  s.dependency 'Capacitor'
  s.swift_version = '5.1'
  s.vendored_frameworks = 'ios/Frameworks/Mia.xcframework'
  s.pod_target_xcconfig = { 'STRIP_BITCODE_FROM_COPIED_FILES' => 'YES' }
end
