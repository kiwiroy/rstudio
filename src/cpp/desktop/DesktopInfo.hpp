/*
 * DesktopInfo.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef DESKTOP_DESKTOP_INFO_HPP
#define DESKTOP_DESKTOP_INFO_HPP

#include <QObject>
#include <QString>

#include "DesktopGwtCallback.hpp"

namespace rstudio {
namespace desktop {

class DesktopInfo : public QObject
{
    Q_OBJECT

Q_SIGNALS:
   void sumatraPdfExePathChanged(QString value);
   void fixedWidthFontListChanged(QString value);
   void zoomLevelChanged(double value);
   void chromiumDevtoolsPortChanged(quint16 value);

public:
   explicit DesktopInfo(QObject* parent = nullptr);

   Q_INVOKABLE QString getPlatform();
   Q_PROPERTY(QString platform READ getPlatform CONSTANT)

   Q_INVOKABLE QString getVersion();
   Q_PROPERTY(QString version READ getVersion CONSTANT)

   Q_INVOKABLE QString getScrollingCompensationType();
   Q_PROPERTY(QString scrollingCompensationType READ getScrollingCompensationType CONSTANT)

   Q_INVOKABLE QString getFixedWidthFontList();
   Q_INVOKABLE void setFixedWidthFontList(QString list);
   Q_PROPERTY(QString fixedWidthFontList
              READ getFixedWidthFontList
              WRITE setFixedWidthFontList
              NOTIFY fixedWidthFontListChanged)

   Q_INVOKABLE QString getFixedWidthFont();
   Q_PROPERTY(QString fixedWidthFont READ getFixedWidthFont CONSTANT)

   Q_INVOKABLE QString getProportionalFont();
   Q_PROPERTY(QString proportionalFont READ getProportionalFont CONSTANT)

   Q_INVOKABLE QString getDesktopSynctexViewer();
   Q_PROPERTY(QString desktopSynctexViewer READ getDesktopSynctexViewer CONSTANT)

   Q_INVOKABLE bool desktopHooksAvailable();
   Q_PROPERTY(bool desktopHooksAvailable READ desktopHooksAvailable CONSTANT)

   Q_INVOKABLE QString getSumatraPdfExePath();
   Q_INVOKABLE void setSumatraPdfExePath(QString path);
   Q_PROPERTY(QString sumatraPdfExePath
              READ getSumatraPdfExePath
              WRITE setSumatraPdfExePath
              NOTIFY sumatraPdfExePathChanged)

   Q_INVOKABLE double getZoomLevel();
   Q_INVOKABLE void setZoomLevel(double zoomLevel);
   Q_PROPERTY(double zoomLevel
              READ getZoomLevel
              WRITE setZoomLevel
              NOTIFY zoomLevelChanged)
   
   Q_INVOKABLE int getChromiumDevtoolsPort();
   Q_INVOKABLE void setChromiumDevtoolsPort(int port);
   Q_PROPERTY(int chromiumDevtoolsPort
              READ getChromiumDevtoolsPort
              WRITE setChromiumDevtoolsPort
              NOTIFY chromiumDevtoolsPortChanged)
};

inline DesktopInfo& desktopInfo()
{
   static DesktopInfo instance;
   return instance;
}

} // end namespace desktop
} // end namespace rstudio

#endif /* DESKTOP_DESKTOP_INFO_HPP */

